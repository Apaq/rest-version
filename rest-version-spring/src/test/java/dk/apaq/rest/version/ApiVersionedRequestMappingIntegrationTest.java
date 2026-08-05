package dk.apaq.rest.version;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = ApiVersionedRequestMappingIntegrationTest.TestConfig.class,
    properties = "spring.main.allow-bean-definition-overriding=true")
class ApiVersionedRequestMappingIntegrationTest {

    static {
        registerVersions();
    }

    private static void registerVersions() {
        ApiVersion.registerVersion(new ApiVersion("2023-01-01"), false);
        ApiVersion.registerVersion(new ApiVersion("2023-02-01"), false);
        ApiVersion.registerVersion(new ApiVersion("2024-01-01"), false);
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {

        @Bean
        CatsV1 catsV1() {
            return new CatsV1();
        }

        @Bean
        CatsV2 catsV2() {
            return new CatsV2();
        }

        @Bean
        MethodVersionedController methodVersionedController() {
            return new MethodVersionedController();
        }
    }

    @RestController
    @ApiVersionedResource(version = "2023-01-01")
    static class CatsV1 {

        @GetMapping("/cats")
        public String getCats() {
            return "v1";
        }
    }

    @RestController
    @ApiVersionedResource(version = "2023-02-01")
    static class CatsV2 {

        @GetMapping("/cats")
        public String getCats() {
            return "v2";
        }
    }

    @RestController
    @ApiVersionedResource(version = "2023-01-01")
    static class MethodVersionedController {

        @ApiVersionedResource(version = "2023-02-01", path = "/method-versioned", method = RequestMethod.GET)
        public String get() {
            return "method-v2";
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        if (ApiVersion.getVersions().isEmpty()) {
            registerVersions();
        }
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void autoConfigurationRegistersVersionedHandlerMapping() {
        assertTrue(handlerMapping instanceof ApiVersionedRequestMapping,
            "Expected the auto-configured handler mapping to be an ApiVersionedRequestMapping");
    }

    @Test
    void exactMatch() throws Exception {
        mockMvc.perform(get("/cats").header("Api-Version", "2023-01-01"))
            .andExpect(content().string("v1"));
        mockMvc.perform(get("/cats").header("Api-Version", "2023-02-01"))
            .andExpect(content().string("v2"));
    }

    @Test
    void fallsBackToClosestEarlierVersion() throws Exception {
        mockMvc.perform(get("/cats").header("Api-Version", "2023-01-15"))
            .andExpect(content().string("v1"));
        mockMvc.perform(get("/cats").header("Api-Version", "2023-02-10"))
            .andExpect(content().string("v2"));
    }

    @Test
    void unknownDateServesLatestAvailableController() throws Exception {
        mockMvc.perform(get("/cats").header("Api-Version", "2024-01-01"))
            .andExpect(content().string("v2"));
        mockMvc.perform(get("/cats").header("Api-Version", "2025-01-01"))
            .andExpect(content().string("v2"));
    }

    @Test
    void missingHeaderUsesDefaultVersion() throws Exception {
        mockMvc.perform(get("/cats"))
            .andExpect(content().string("v1"));
    }

    @Test
    void unparseableHeaderUsesDefaultVersion() throws Exception {
        mockMvc.perform(get("/cats").header("Api-Version", "not-a-date"))
            .andExpect(content().string("v1"));
    }

    @Test
    void methodLevelVersionOverridesTypeLevel() throws Exception {
        mockMvc.perform(get("/method-versioned").header("Api-Version", "2023-02-01"))
            .andExpect(content().string("method-v2"));
        mockMvc.perform(get("/method-versioned").header("Api-Version", "2023-01-01"))
            .andExpect(status().isNotFound());
    }
}
