package dk.apaq.rest.version.quarkus;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Bytecode recorder that materialises the {@link VersionedResourceRegistry} at runtime init
 * from data gathered by the deployment build step.
 */
@Recorder
public class RestVersionRecorder {

    /**
     * Creates the registry of versioned resources.
     *
     * @param versionedPaths    The logical (clean) paths that are served by versioned resources.
     * @param referencedVersions The versions referenced by {@link ApiVersionedResource} annotations.
     * @return A runtime value holding the registry.
     */
    public RuntimeValue<VersionedResourceRegistry> createVersionedResourceRegistry(
            String[] versionedPaths, String[] referencedVersions) {
        return new RuntimeValue<>(new VersionedResourceRegistry(versionedPaths, referencedVersions));
    }
}
