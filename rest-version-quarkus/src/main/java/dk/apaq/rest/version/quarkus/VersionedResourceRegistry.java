package dk.apaq.rest.version.quarkus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dk.apaq.rest.version.ApiVersion;

/**
 * Immutable, build-time populated registry of the versioned resources declared by the application.
 *
 * <p>The instance is produced by a bytecode recorder at static init and exposed as a synthetic
 * CDI bean, so it can be injected into the {@link VersioningRequestFilter} without any runtime
 * reflection.</p>
 */
public class VersionedResourceRegistry {

    private final List<String> versionedPaths;
    private final List<LocalDate> controllerVersions;
    private final Set<String> referencedVersions;

    public VersionedResourceRegistry(String[] versionedPaths, String[] referencedVersions) {
        List<String> paths = new ArrayList<>(Arrays.asList(versionedPaths));
        // longest first so that the most specific path wins
        paths.sort(Comparator.comparingInt(String::length).reversed());
        this.versionedPaths = Collections.unmodifiableList(paths);

        List<LocalDate> dates = new ArrayList<>(referencedVersions.length);
        for (String version : referencedVersions) {
            dates.add(LocalDate.parse(version));
        }
        Collections.sort(dates);
        this.controllerVersions = Collections.unmodifiableList(dates);
        this.referencedVersions = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(referencedVersions)));
    }

    /**
     * Checks whether the given request path (relative to the application root, no leading
     * slash) belongs to a versioned resource.
     */
    public boolean isVersionedPath(String relativePath) {
        for (String path : versionedPaths) {
            if (path.isEmpty()) {
                if (relativePath.isEmpty()) {
                    return true;
                }
                continue;
            }
            if (relativePath.equals(path) || relativePath.startsWith(path + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the version of the controller that should serve a request resolved to the given
     * date: the latest controller version that is equal to or earlier than the resolved date.
     *
     * @param resolvedDate The version the request was resolved to (a registered version).
     * @return The controller version to route to, or {@code null} if none is earlier than or
     *         equal to the resolved date.
     */
    public ApiVersion findRouteVersion(LocalDate resolvedDate) {
        for (int i = controllerVersions.size() - 1; i >= 0; i--) {
            LocalDate date = controllerVersions.get(i);
            if (!date.isAfter(resolvedDate)) {
                return new ApiVersion(date);
            }
        }
        return null;
    }

    /**
     * Returns the API versions referenced by {@link ApiVersionedResource} annotations.
     */
    public Set<String> getReferencedVersions() {
        return referencedVersions;
    }
}
