package dk.apaq.rest.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class represents an API version, allowing for version registration, retrieval, and management.
 * Each version is associated with a specific date and can be identified by this date.
 *
 * <p>All supported versions must be registered at application startup, before the web context is
 * built. The registry is thread-safe.</p>
 */
public class ApiVersion {

    // Formatter for parsing and formatting dates in the 'yyyy-MM-dd' format.
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // A list of registered API versions.
    private static final List<ApiVersion> VERSIONS = new CopyOnWriteArrayList<>();

    // Logger for this class.
    private final static Logger LOG = LoggerFactory.getLogger(ApiVersion.class);

    // The default API version. This is either the first registered version or a manually set version.
    private static volatile ApiVersion DEFAULT_VERSION;

    /**
     * Registers a new API version.
     *
     * @param version        The ApiVersion object to register.
     * @throws IllegalArgumentException If the version is already registered.
     */
    public static void registerVersion(ApiVersion version) {
        ApiVersion.registerVersion(version, false);
    }

    /**
     * Registers a new API version.
     *
     * @param version        The ApiVersion object to register.
     * @param defaultVersion Whether this version should be the default version.
     * @throws IllegalArgumentException If the version is already registered.
     */
    public static void registerVersion(ApiVersion version, boolean defaultVersion) {
        if (version == null) {
            throw new IllegalArgumentException("Version must not be null");
        }
        for (ApiVersion registered : VERSIONS) {
            if (registered.getVersionDate().equals(version.getVersionDate())) {
                throw new IllegalArgumentException("Version '" + version.getVersion() + "' is already registered");
            }
        }
        VERSIONS.add(version);
        // Set this version as the default if specified or if no default version is set.
        if(defaultVersion || DEFAULT_VERSION == null) {
            ApiVersion.DEFAULT_VERSION = version;
        }
    }

    /**
     * Sets the default API version explicitly.
     *
     * @param version The ApiVersion object to use as the default.
     * @throws IllegalArgumentException If the version is not registered.
     */
    public static void setDefaultVersion(ApiVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("Version must not be null");
        }
        if (!VERSIONS.contains(version)) {
            throw new IllegalArgumentException("Version '" + version.getVersion() + "' is not registered");
        }
        ApiVersion.DEFAULT_VERSION = version;
    }

    /**
     * Clears all registered versions.
     */
    public static void clear() {
        ApiVersion.VERSIONS.clear();
        ApiVersion.DEFAULT_VERSION = null;
    }

    // The version string (formatted as yyyy-MM-dd).
    private final String version;

    // The date representing the API version.
    private final LocalDate versionDate;

    /**
     * Constructs an ApiVersion from a date string in the format 'yyyy-MM-dd'.
     *
     * @param date The version date string.
     */
    public ApiVersion(String date) {
        this(LocalDate.parse(date));
    }

    /**
     * Constructs an ApiVersion from year, month, and day values.
     *
     * @param year  The year of the version.
     * @param month The month of the version.
     * @param day   The day of the version.
     */
    public ApiVersion(int year, int month, int day) {
        this(LocalDate.of(year, month, day));
    }

    /**
     * Constructs an ApiVersion from a LocalDate object.
     *
     * @param versionDate The LocalDate representing the version.
     */
    public ApiVersion(LocalDate versionDate) {
        this.versionDate = versionDate;
        this.version = versionDate.format(dateFormatter);
    }

    /**
     * Returns the version string in 'yyyy-MM-dd' format.
     *
     * @return The version string.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the LocalDate representing the version.
     *
     * @return The version date.
     */
    public LocalDate getVersionDate() {
        return versionDate;
    }

    /**
     * Returns the first registered API version.
     *
     * @return The first ApiVersion object.
     */
    public static ApiVersion getFirstVersion() {
        if(VERSIONS.isEmpty()) {
            throw new IllegalStateException("No versions registered");
        }
        return VERSIONS.get(0);
    }

    /**
     * Returns an unmodifiable list of all registered API versions.
     *
     * @return A list of ApiVersion objects.
     */
    public static List<ApiVersion> getVersions() {
        return Collections.unmodifiableList(VERSIONS);
    }

    /**
     * Returns the last registered API version, which is considered the default version if not manually set.
     *
     * @return The default ApiVersion object.
     */
    public static ApiVersion getDefaultVersion() {
        return DEFAULT_VERSION;
    }

    /**
     * Retrieves an ApiVersion that matches or is closest to the provided version string.
     * If no exact match is found, the closest earlier version is returned. If no registered
     * version is earlier than the requested one, the first registered version is returned.
     *
     * @param version The version date string to match against.
     * @return The closest matching ApiVersion object.
     */
    public static ApiVersion from(String version) {
        try {
            LocalDate date = LocalDate.parse(version);
            ApiVersion closest = null;

            // Find the registered version that is not after the requested date and closest to it.
            for(ApiVersion current : VERSIONS) {
                if(!current.getVersionDate().isAfter(date)) {
                    if(closest == null || current.getVersionDate().isAfter(closest.getVersionDate())) {
                        closest = current;
                    }
                }
            }
            return closest != null ? closest : getFirstVersion();
        } catch (Exception ex) {
            // If parsing fails or no matching version is found, return the default version.
            return getDefaultVersion();
        }
    }
}
