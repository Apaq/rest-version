package dk.apaq.rest.version;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * Custom handler mapping that supports API versioning through the {@link ApiVersionedResource} annotation.
 * This class extends {@link RequestMappingHandlerMapping} to apply version-based request conditions
 * to controllers and methods marked with the `ApiVersionedResource` annotation.
 */
public class ApiVersionedRequestMapping extends RequestMappingHandlerMapping {

    /**
     * Creates a version-aware request mapping handler. The order is set to {@code 0} to
     * match Spring Boot's default {@code requestMappingHandlerMapping} bean, ensuring it
     * is consulted before resource and static-content handlers.
     */
    public ApiVersionedRequestMapping() {
        setOrder(0);
    }

    // The request header that carries the API version.
    private String headerName = ApiVersionedResourceRequestCondition.HEADER_VERSION;

    /**
     * Returns the request header that carries the API version.
     *
     * @return The header name, defaulting to {@code Api-Version}.
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * Sets the request header that carries the API version.
     *
     * @param headerName The header name to use.
     */
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    /**
     * Retrieves the custom request condition for a given handler type (class-level).
     * Checks if the class is annotated with {@link ApiVersionedResource}, and if so,
     * creates a custom {@link ApiVersionedResourceRequestCondition} based on the version defined in the annotation.
     *
     * @param handlerType The handler class type to check for custom conditions.
     * @return A custom {@link RequestCondition} for the class if annotated, otherwise the default condition.
     */
    @Override
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        ApiVersionedResource typeAnnotation = AnnotationUtils.findAnnotation(handlerType, ApiVersionedResource.class);
        return createCondition(typeAnnotation, super.getCustomTypeCondition(handlerType), false);
    }

    /**
     * Retrieves the custom request condition for a given method (method-level).
     * Checks if the method is annotated with {@link ApiVersionedResource}, and if so,
     * creates a custom {@link ApiVersionedResourceRequestCondition} based on the version defined in the annotation.
     *
     * @param method The method to check for custom conditions.
     * @return A custom {@link RequestCondition} for the method if annotated, otherwise the default condition.
     */
    @Override
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        ApiVersionedResource methodAnnotation = AnnotationUtils.findAnnotation(method, ApiVersionedResource.class);
        return createCondition(methodAnnotation, super.getCustomMethodCondition(method), true);
    }

    /**
     * Creates a custom request condition based on the {@link ApiVersionedResource} annotation.
     * If the annotation is present, it creates a new {@link ApiVersionedResourceRequestCondition}
     * with the specified version. Otherwise, it falls back to the provided default condition.
     *
     * @param versionMapping   The {@link ApiVersionedResource} annotation, if present.
     * @param defaultCondition The default {@link RequestCondition} to use if no annotation is found.
     * @param fromMethod       Whether the annotation originates from a method-level declaration.
     * @return A custom request condition if the annotation is present, otherwise the default condition.
     */
    private RequestCondition<?> createCondition(ApiVersionedResource versionMapping, RequestCondition<?> defaultCondition, boolean fromMethod) {
        if (versionMapping != null) {
            return new ApiVersionedResourceRequestCondition(
                Collections.singleton(versionMapping.version()), fromMethod, headerName);
        }
        return defaultCondition;
    }
}
