package dk.apaq.rest.version;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiVersionedRequestMappingTest {

    private ApiVersionedRequestMapping handlerMapping;

    @BeforeEach
    void setUp() {
        ApiVersion.clear();
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

    @Test
    void testGetCustomMethodCondition_WithAnnotation() throws NoSuchMethodException {
        ApiVersion.registerVersion(new ApiVersion("2023-01-01"), true);
        ApiVersion.registerVersion(new ApiVersion("2024-01-01"), false);

        Method method = MethodVersionedController.class.getMethod("get");

        RequestCondition<?> condition = handlerMapping.getCustomMethodCondition(method);

        assertNotNull(condition);
        assertTrue(condition instanceof ApiVersionedResourceRequestCondition);

        ApiVersionedResourceRequestCondition versionCondition = (ApiVersionedResourceRequestCondition) condition;
        assertEquals("2024-01-01", versionCondition.getLatestVersion().getVersion());
    }

    @Test
    void testMethodVersionOverridesTypeVersion() throws NoSuchMethodException {
        ApiVersion.registerVersion(new ApiVersion("2023-01-01"), true);
        ApiVersion.registerVersion(new ApiVersion("2024-01-01"), false);

        RequestCondition<?> typeCondition = handlerMapping.getCustomTypeCondition(MethodVersionedController.class);
        RequestCondition<?> methodCondition = handlerMapping.getCustomMethodCondition(
            MethodVersionedController.class.getMethod("get"));

        assertNotNull(typeCondition);
        assertNotNull(methodCondition);

        ApiVersionedResourceRequestCondition combined = ((ApiVersionedResourceRequestCondition) typeCondition)
            .combine((ApiVersionedResourceRequestCondition) methodCondition);

        assertEquals("2024-01-01", combined.getLatestVersion().getVersion());
    }

    @Test
    void testCustomHeaderNameIsUsed() {
        handlerMapping.setHeaderName("X-API-Version");
        assertEquals("X-API-Version", handlerMapping.getHeaderName());
    }



    // Mock controller with ApiVersionedResource annotation
    @RequestMapping("/mocks")
    @ApiVersionedResource(version = "2023-01-01")
    private static class MockControllerV1 {

        @GetMapping
        public String get() {
            return "mockv1";
        }
    }

    // Mock controller with ApiVersionedResource annotation
    @RequestMapping("/mocks")
    @ApiVersionedResource(version = "2024-01-01")
    private static class MockControllerV2 {

        @GetMapping
        public String get() {
            return "mockv2";
        }
    }


    // Mock controller without ApiVersionedResource annotation
    private static class NonAnnotatedController {

    }

    // Mock controller with both a type-level and a method-level version
    @ApiVersionedResource(version = "2023-01-01")
    private static class MethodVersionedController {

        @ApiVersionedResource(version = "2024-01-01", path = "/mocks", method = RequestMethod.GET)
        public String get() {
            return "mockv2";
        }
    }
}
