package dk.apaq.rest.version;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a controller or method as part of a specific API version.
 * This annotation is used in combination with the {@link ApiVersionedRequestMapping}
 * to map requests based on the API version defined.
 *
 * <p>The version is specified using an ISO date format (yyyy-MM-dd) and can be applied
 * at both the class level (for a controller) and method level. When applied to a
 * method it overrides any version declared at the class level for that method.</p>
 *
 * <p>Because this annotation is meta-annotated with {@link RequestMapping}, applying it
 * to a method also contributes the mapping details. Use the {@link #path()} and
 * {@link #method()} attributes on method-level declarations instead of combining it with
 * {@code @GetMapping} or {@code @RequestMapping}, as multiple {@code @RequestMapping}
 * annotations on the same method are not supported by Spring.</p>
 */
@RequestMapping
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
     * Optional additional path appended after the version
     */
    @AliasFor(annotation = RequestMapping.class, attribute = "path")
    String path() default "";

    /**
     * The HTTP request methods mapped by this resource.
     */
    @AliasFor(annotation = RequestMapping.class, attribute = "method")
    RequestMethod[] method() default {};

}
