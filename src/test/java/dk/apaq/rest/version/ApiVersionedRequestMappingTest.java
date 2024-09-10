package dk.apaq.rest.version;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiVersionedRequestMappingTest {

    private ApiVersionedRequestMapping handlerMapping;

    @BeforeEach
    void setUp() {
        handlerMapping = new ApiVersionedRequestMapping();
    }

    @Test
    void testGetCustomTypeCondition_WithAnnotation() {
        // Mock a class with the ApiVersionedResource annotation
        Class<?> annotatedClass = MockControllerV1.class;

        // Register version
        ApiVersion.registerVersion(new ApiVersion("2023-01-01"), true);

        // Get the custom condition for the class
        RequestCondition<?> condition = handlerMapping.getCustomTypeCondition(annotatedClass);

        // Verify that a condition is created and it matches the version
        assertNotNull(condition);
        assertTrue(condition instanceof ApiVersionedResourceRequestCondition);

        ApiVersionedResourceRequestCondition versionCondition = (ApiVersionedResourceRequestCondition) condition;
        assertEquals("2023-01-01", versionCondition.getLatestVersion().getVersion());
    }

    @Test
    void testGetCustomTypeCondition_WithoutAnnotation() {
        // Mock a class without the ApiVersionedResource annotation
        Class<?> nonAnnotatedClass = NonAnnotatedController.class;

        // Get the custom condition for the class
        RequestCondition<?> condition = handlerMapping.getCustomTypeCondition(nonAnnotatedClass);

        // Verify that no custom condition is created
        assertNull(condition);
    }



    // Mock controller with ApiVersionedResource annotation
    @ApiVersionedResource(version = "2023-01-01")
    private static class MockControllerV1 {

    }

    // Mock controller without ApiVersionedResource annotation
    private static class NonAnnotatedController {

    }
}
