package dk.apaq.rest.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class ApiVersionedResourceRequestCondition extends AbstractRequestCondition<ApiVersionedResourceRequestCondition> {

    public static final String HEADER_VERSION = "Api-Version";
    private final static Logger LOG = LoggerFactory.getLogger(ApiVersionedResourceRequestCondition.class);
    private final Set<ApiVersion> versions;


    public ApiVersionedResourceRequestCondition(String version) {
        this(Collections.singletonList(version));
    }

    public ApiVersionedResourceRequestCondition(Collection<String> versions) {
        this.versions = Collections.unmodifiableSet(toVersionSet(versions));
    }

    public ApiVersionedResourceRequestCondition(Set<ApiVersion> versions) {
        this.versions = Collections.unmodifiableSet(versions);
    }

    @Override
    public ApiVersionedResourceRequestCondition combine(ApiVersionedResourceRequestCondition other) {
        LOG.debug("Combining:\n{}\n{}", this, other);
        Set<ApiVersion> newVersions = new LinkedHashSet<>(this.versions);
        newVersions.addAll(other.versions);
        return new ApiVersionedResourceRequestCondition(newVersions);
    }

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

    public ApiVersion getLatestVersion() {
        ApiVersion result = null;
        for(ApiVersion version : versions) {
            if(result == null || version.getVersionDate().isAfter(result.getVersionDate())) {
                result = version;
            }
        }
        return result;
    }

    @Override
    protected Collection<?> getContent() {
        return versions;
    }

    @Override
    protected String getToStringInfix() {
        return " && ";
    }

    @Override
    public int compareTo(ApiVersionedResourceRequestCondition other, HttpServletRequest request) {
        return other.getLatestVersion().getVersionDate().compareTo(this.getLatestVersion().getVersionDate());
    }

    private static Set<ApiVersion> toVersionSet(Collection<String> versions) {
        Set<ApiVersion> result = new HashSet<>();

        for(String version : versions) {
            result.add(ApiVersion.from(version));
        }

        return result;
    }

}