package dk.apaq.rest.version.quarkus;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for the rest-version Quarkus extension.
 *
 * <pre>
 * quarkus.rest-version.header-name=Api-Version
 * quarkus.rest-version.enabled=true
 * </pre>
 */
@ConfigMapping(prefix = "quarkus.rest-version")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface RestVersionConfig {

    /**
     * The request header that carries the API version.
     */
    @WithDefault("Api-Version")
    String headerName();

    /**
     * Whether version routing is enabled.
     */
    @WithDefault("true")
    boolean enabled();
}
