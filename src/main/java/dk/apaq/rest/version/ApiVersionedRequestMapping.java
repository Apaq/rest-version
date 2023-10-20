package dk.apaq.rest.version;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

public class ApiVersionedRequestMapping extends RequestMappingHandlerMapping {

    @Override
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        ApiVersionedResource typeAnnotation = AnnotationUtils.findAnnotation(handlerType, ApiVersionedResource.class);
        return createCondition(typeAnnotation, super.getCustomTypeCondition(handlerType));
    }

    @Override
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        ApiVersionedResource methodAnnotation = AnnotationUtils.findAnnotation(method.getClass(), ApiVersionedResource.class);
        return createCondition(methodAnnotation, super.getCustomMethodCondition(method));
    }

    private RequestCondition<?> createCondition(ApiVersionedResource versionMapping, RequestCondition<?> defaultCondition) {
        if (versionMapping != null) {
            return new ApiVersionedResourceRequestCondition(versionMapping.version());
        }

        return defaultCondition;
    }
}