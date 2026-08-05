package dk.apaq.rest.version.quarkus.deployment;

import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.resteasy.reactive.spi.AdditionalResourceClassBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerRequestFilterBuildItem;
import dk.apaq.rest.version.quarkus.ApiVersionedResource;
import dk.apaq.rest.version.quarkus.RestVersionRecorder;
import dk.apaq.rest.version.quarkus.VersionedResourceRegistry;
import dk.apaq.rest.version.quarkus.VersioningRequestFilter;

/**
 * Build-time processing for the rest-version Quarkus extension.
 *
 * <p>Classes annotated with {@link ApiVersionedResource} are registered as Jakarta REST
 * resources under a versioned path ({@code /v<yyyyMMdd><path>}) via
 * {@link AdditionalResourceClassBuildItem}. The versioned paths and the referenced versions
 * are recorded so the runtime {@link VersioningRequestFilter} can route requests based on the
 * {@code Api-Version} header.</p>
 */
public class RestVersionProcessor {

    private static final DotName API_VERSIONED_RESOURCE = DotName.createSimple(ApiVersionedResource.class.getName());
    private static final DotName JAXRS_PATH = DotName.createSimple(Path.class.getName());

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    @BuildStep
    public void scanAndRegisterResources(CombinedIndexBuildItem combinedIndex,
            BuildProducer<VersionedResourceScanBuildItem> scanProducer,
            BuildProducer<AdditionalResourceClassBuildItem> additionalResources,
            BuildProducer<CustomContainerRequestFilterBuildItem> filters) {
        IndexView index = combinedIndex.getIndex();
        VersionedResourceScan scan = new VersionedResourceScan();

        for (AnnotationInstance instance : index.getAnnotations(API_VERSIONED_RESOURCE)) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.METHOD) {
                MethodInfo method = target.asMethod();
                throw new IllegalStateException("Method-level '@ApiVersionedResource' is not supported by the Quarkus "
                        + "rest-version extension. Move '" + method.name() + "' of " + method.declaringClass().name()
                        + " to its own controller annotated with '@ApiVersionedResource'.");
            }
            ClassInfo clazz = target.asClass();
            if (Modifier.isInterface(clazz.flags())) {
                continue;
            }
            if (clazz.declaredAnnotation(JAXRS_PATH) != null) {
                throw new IllegalStateException("Class '" + clazz.name()
                        + "' declares both '@Path' and '@ApiVersionedResource'. With the Quarkus rest-version extension "
                        + "the resource path is provided through the 'path' attribute of '@ApiVersionedResource'.");
            }

            String version = instance.value("version").asString();
            AnnotationValue pathValue = instance.value("path");
            String cleanPath = normalizePath(pathValue == null ? "" : pathValue.asString());
            String versionedPath = "/v" + basicDate(version) + (cleanPath.isEmpty() ? "" : "/" + cleanPath);

            additionalResources.produce(new AdditionalResourceClassBuildItem(clazz, versionedPath));
            scan.getVersionedPaths().add(cleanPath);
            scan.getReferencedVersions().add(version);
        }

        filters.produce(new CustomContainerRequestFilterBuildItem(VersioningRequestFilter.class.getName()));
        scanProducer.produce(new VersionedResourceScanBuildItem(scan));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public SyntheticBeanBuildItem registerVersionedResourceRegistry(VersionedResourceScanBuildItem scan,
            RestVersionRecorder recorder) {
        VersionedResourceScan data = scan.getScan();
        return SyntheticBeanBuildItem.configure(VersionedResourceRegistry.class)
                .scope(Singleton.class)
                .runtimeValue(recorder.createVersionedResourceRegistry(
                        data.getVersionedPaths().toArray(new String[0]),
                        data.getReferencedVersions().toArray(new String[0])))
                .done();
    }

    private static String basicDate(String version) {
        try {
            return LocalDate.parse(version).format(BASIC_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid API version '" + version
                    + "' in '@ApiVersionedResource'. Versions must be ISO dates (yyyy-MM-dd).", e);
        }
    }

    private static String normalizePath(String path) {
        String result = path == null ? "" : path;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
