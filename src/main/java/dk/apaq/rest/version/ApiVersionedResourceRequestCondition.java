package dk.apaq.rest.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Custom request condition that checks for the API version in the request header
 * and determines whether the handler method should handle the request based on the API version.
 * It works with the {@link ApiVersionedResource} annotation to enforce versioning on controllers and methods.
 */
public class ApiVersionedResourceRequestCondition extends AbstractRequestCondition<ApiVersionedResourceRequestCondition> {

    // Header key used to specify API version in the request.
    public static final String HEADER_VERSION = "Api-Version";

    // Logger for this class.
    private final static Logger LOG = LoggerFactory.getLogger(ApiVersionedResourceRequestCondition.class);

    // Set of supported API versions for the condition.
    private final Set<ApiVersion> versions;

    /**
     * Constructor that accepts a single API version.
     *
     * @param version The version string to initialize the condition.
     */
    public ApiVersionedResourceRequestCondition(String version) {
        this(Collections.singletonList(version));
    }

    /**
     * Constructor that accepts a collection of API versions.
     *
     * @param versions The collection of version strings to initialize the condition.
     */
    public ApiVersionedResourceRequestCondition(Collection<String> versions) {
        this.versions = Collections.unmodifiableSet(toVersionSet(versions));
    }

    /**
     * Constructor that accepts a set of {@link ApiVersion} objects.
     *
     * @param versions The set of {@link ApiVersion} objects to initialize the condition.
     */
    public ApiVersionedResourceRequestCondition(Set<ApiVersion> versions) {
        this.versions = Collections.unmodifiableSet(versions);
    }

    /**
     * Combines this condition with another condition by merging their version sets.
     *
     * @param other The other {@link ApiVersionedResourceRequestCondition} to combine with.
     * @return A new {@link ApiVersionedResourceRequestCondition} containing the merged version set.
     */
    @Override
    public ApiVersionedResourceRequestCondition combine(ApiVersionedResourceRequestCondition other) {
        LOG.debug("Combining:\n{}\n{}", this, other);
        Set<ApiVersion> newVersions = new LinkedHashSet<>(this.versions);
        newVersions.addAll(other.versions);
        return new ApiVersionedResourceRequestCondition(newVersions);
    }

    /**
     * Checks if the request matches any of the versions specified in this condition.
     * It reads the version from the "Api-Version" header and matches it against the versions
     * in this condition. If no version header is present, the default API version is used.
     *
     * @param request The {@link HttpServletRequest} to match against.
     * @return The current condition if a matching version is found, otherwise {@code null}.
     */
    @Override
    public ApiVersionedResourceRequestCondition getMatchingCondition(HttpServletRequest request) {
        final String header = request.getHeader(HEADER_VERSION);
        LOG.debug("Api-Version header = {}", header);

        var version = StringUtils.hasLength(header) ? ApiVersion.from(header) : ApiVersion.getDefaultVersion();

        if (version != null) {
            for(ApiVersion current : versions) {
                if(!version.getVersionDate().isBefore(current.getVersionDate())) {
                    return this;
                }
            }
            LOG.debug("Unable to find a matching version");
        }
        return null;
    }

    /**
     * Retrieves the latest version from the set of versions in this condition.
     *
     * @return The latest {@link ApiVersion} in the set.
     */
    public ApiVersion getLatestVersion() {
        ApiVersion result = null;
        for(ApiVersion version : versions) {
            if(result == null || version.getVersionDate().isAfter(result.getVersionDate())) {
                result = version;
            }
        }
        return result;
    }

    /**
     * Returns the content of this condition, which is the set of API versions.
     *
     * @return A collection of API versions.
     */
    @Override
    protected Collection<?> getContent() {
        return versions;
    }

    /**
     * Returns the infix used when converting this condition to a string.
     *
     * @return The string " && ".
     */
    @Override
    protected String getToStringInfix() {
        return " && ";
    }

    /**
     * Compares this condition with another to determine which has the latest version.
     * This is used to prioritize handlers when multiple conditions match a request.
     *
     * @param other   The other {@link ApiVersionedResourceRequestCondition} to compare with.
     * @param request The current {@link HttpServletRequest}.
     * @return A negative value if this condition has an earlier version, zero if the versions are the same,
     *         or a positive value if this condition has a later version.
     */
    @Override
    public int compareTo(ApiVersionedResourceRequestCondition other, HttpServletRequest request) {
        return other.getLatestVersion().getVersionDate().compareTo(this.getLatestVersion().getVersionDate());
    }

    /**
     * Converts a collection of version strings into a set of {@link ApiVersion} objects.
     *
     * @param versions The collection of version strings to convert.
     * @return A set of {@link ApiVersion} objects.
     */
    private static Set<ApiVersion> toVersionSet(Collection<String> versions) {
        Set<ApiVersion> result = new HashSet<>();

        for(String version : versions) {
            result.add(ApiVersion.from(version));
        }

        return result;
    }
}
