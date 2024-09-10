package dk.apaq.rest.version;

import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a controller or method as part of a specific API version.
 * This annotation is used in combination with the {@link ApiVersionedRequestMapping}
 * to map requests based on the API version defined.
 *
 * The version is specified using an ISO date format (yyyy-MM-dd) and can be applied
 * at both the class level (for a controller) and method level.
 */
@RequestMapping
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersionedResource {

    /**
     * Defines the version of the API in ISO date format (yyyy-MM-dd).
     *
     * @return The version date for the API resource.
     */
    String version();
}
