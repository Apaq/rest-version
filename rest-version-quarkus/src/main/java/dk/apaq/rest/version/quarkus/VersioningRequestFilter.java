package dk.apaq.rest.version.quarkus;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.apaq.rest.version.ApiVersion;

/**
 * Pre-matching request filter that routes requests to the correct API version.
 *
 * <p>Clients call the logical path (e.g. {@code /cats}) and select a version through the
 * {@code Api-Version} header. The filter resolves the version against the registered versions
 * (falling back to the closest earlier version and to the default version when the header is
 * missing or invalid) and rewrites the request to the versioned resource path, e.g.
 * {@code /cats} to {@code /v20230201/cats}.</p>
 */
@Singleton
public class VersioningRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(VersioningRequestFilter.class);

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    @Inject
    @ConfigProperty(name = "quarkus.rest-version.header-name", defaultValue = "Api-Version")
    String headerName;

    @Inject
    @ConfigProperty(name = "quarkus.rest-version.enabled", defaultValue = "true")
    boolean enabled;

    @Inject
    VersionedResourceRegistry registry;

    private volatile boolean validated;

    @ServerRequestFilter(preMatching = true)
    public void routeVersionedRequest(ContainerRequestContext requestContext) {
        if (!enabled) {
            return;
        }
        validateVersions();

        UriInfo uriInfo = requestContext.getUriInfo();
        String relativePath = uriInfo.getPath();
        // in a pre-matching filter the path is reported with a leading slash
        String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        if (!registry.isVersionedPath(cleanPath)) {
            return;
        }

        final String header = requestContext.getHeaderString(headerName);
        ApiVersion resolved = (header != null && !header.isEmpty())
                ? ApiVersion.from(header)
                : ApiVersion.getDefaultVersion();
        if (resolved == null) {
            LOG.debug("Unable to resolve a version from header '{}' for path '{}'", header, cleanPath);
            return;
        }

        ApiVersion routeVersion = registry.findRouteVersion(resolved.getVersionDate());
        if (routeVersion == null) {
            LOG.debug("No versioned controller matches resolved version {} for path '{}'", resolved.getVersion(), cleanPath);
            return;
        }

        String segment = "v" + routeVersion.getVersionDate().format(BASIC_DATE);
        StringBuilder rewritten = new StringBuilder(uriInfo.getBaseUri().getPath())
                .append(segment).append('/').append(cleanPath);
        String query = uriInfo.getRequestUri().getRawQuery();
        if (query != null && !query.isEmpty()) {
            rewritten.append('?').append(query);
        }
        requestContext.setRequestUri(URI.create(rewritten.toString()));
        LOG.debug("Rewrote '{}' to '{}' for version {}", cleanPath, rewritten, routeVersion.getVersion());
    }

    /**
     * Ensures that every version referenced by a {@link ApiVersionedResource} annotation is
     * registered before the first request is routed. Runs lazily on the first request so it
     * works regardless of how the versions are registered (e.g. in {@code main}, a startup
     * bean or a test).
     */
    private void validateVersions() {
        if (validated) {
            return;
        }
        synchronized (this) {
            if (validated) {
                return;
            }
            List<ApiVersion> registered = ApiVersion.getVersions();
            Set<String> missing = new LinkedHashSet<>();
            for (String referenced : registry.getReferencedVersions()) {
                boolean found = false;
                for (ApiVersion version : registered) {
                    if (version.getVersion().equals(referenced)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    missing.add(referenced);
                }
            }
            if (!missing.isEmpty()) {
                throw new IllegalStateException(
                        "The following API versions referenced by @ApiVersionedResource are not registered: " + missing
                                + ". Register all versions before the first request, e.g. ApiVersion.registerVersion(...) "
                                + "in main(). Registered versions: " + registered);
            }
            validated = true;
        }
    }
}
