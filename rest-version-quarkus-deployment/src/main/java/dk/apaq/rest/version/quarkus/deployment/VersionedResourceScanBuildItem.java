package dk.apaq.rest.version.quarkus.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Carries the {@link VersionedResourceScan} from the scanning build step to the build step
 * that records the runtime registry.
 */
public final class VersionedResourceScanBuildItem extends SimpleBuildItem {

    private final VersionedResourceScan scan;

    public VersionedResourceScanBuildItem(VersionedResourceScan scan) {
        this.scan = scan;
    }

    public VersionedResourceScan getScan() {
        return scan;
    }
}
