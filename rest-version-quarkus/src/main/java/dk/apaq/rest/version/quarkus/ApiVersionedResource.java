package dk.apaq.rest.version.quarkus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a controller (Jakarta REST resource) as part of a specific API version.
 *
 * <p>The version is specified using an ISO date format (yyyy-MM-dd). The {@link #path()}
 * attribute defines the logical resource path exactly as clients call it (e.g. {@code /cats});
 * the version segment is added automatically by the build-time integration, so the version
 * number only ever needs to be written once.</p>
 *
 * <p>The extension registers the annotated class as a Jakarta REST resource under the
 * versioned path {@code /v&lt;yyyyMMdd&gt;&lt;path&gt;} and a pre-matching request filter
 * routes incoming requests to it based on the {@code Api-Version} header.</p>
 *
 * <p>Method-level versioning is not supported: a single resource class belongs to a single
 * API version. Introduce a separate controller for each version.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersionedResource {

    /**
     * Defines the version of the API in ISO date format (yyyy-MM-dd).
     *
     * @return The version date for the API resource.
     */
    String version();

    /**
     * The logical path of the resource as clients call it, e.g. {@code /cats}. Must not
     * contain the API version - the version segment is added by the extension.
     *
     * @return The resource path.
     */
    String path() default "";
}
