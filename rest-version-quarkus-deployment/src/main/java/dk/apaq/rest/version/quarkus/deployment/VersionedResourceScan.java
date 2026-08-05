package dk.apaq.rest.version.quarkus.deployment;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Result of scanning the application index for {@code @ApiVersionedResource} annotations.
 */
public class VersionedResourceScan {

    private final Set<String> versionedPaths = new LinkedHashSet<>();
    private final Set<String> referencedVersions = new LinkedHashSet<>();

    public Set<String> getVersionedPaths() {
        return versionedPaths;
    }

    public Set<String> getReferencedVersions() {
        return referencedVersions;
    }
}
