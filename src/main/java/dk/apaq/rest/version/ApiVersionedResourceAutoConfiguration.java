package dk.apaq.rest.version;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Auto-configuration that registers the {@link ApiVersionedRequestMapping} as the
 * application's {@link RequestMappingHandlerMapping}.
 *
 * <p>The bean deliberately uses the conventional name {@code requestMappingHandlerMapping}
 * so that it replaces Spring Boot's default handler mapping. Because Spring Boot's own
 * bean is declared unconditionally, applications must enable bean definition overriding
 * via {@code spring.main.allow-bean-definition-overriding=true}.</p>
 *
 * <p>Registering your own {@link ApiVersionedRequestMapping} bean (or setting
 * {@code rest-version.enabled=false}) disables this auto-configuration.</p>
 */
@AutoConfiguration(after = WebMvcAutoConfiguration.class)
@ConditionalOnProperty(prefix = "rest-version", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApiVersionedResourceAutoConfiguration {

    /**
     * Registers the version-aware request mapping handler.
     *
     * @return The {@link ApiVersionedRequestMapping} to use for handling requests.
     */
    @Bean
    @ConditionalOnMissingBean(ApiVersionedRequestMapping.class)
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        return new ApiVersionedRequestMapping();
    }
}
