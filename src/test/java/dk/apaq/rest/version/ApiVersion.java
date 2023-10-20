package dk.apaq.rest.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApiVersion {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<ApiVersion> VERSIONS = new ArrayList<>();
    private final static Logger LOG = LoggerFactory.getLogger(ApiVersion.class);
    public static ApiVersion DEFAULT_VERSION;

    public static void registerVersion(ApiVersion version, boolean defaultVersion) {
        VERSIONS.add(version);
        if(defaultVersion || DEFAULT_VERSION == null) {
            ApiVersion.DEFAULT_VERSION = version;
        }
    }

    private final String version;
    private final LocalDate versionDate;

    public ApiVersion(String date) {
        this(LocalDate.parse(date));
    }

    public ApiVersion(int year, int month, int day) {
        this(LocalDate.of(year, month, day));
    }

    public ApiVersion(LocalDate versionDate) {
        this.versionDate = versionDate;
        this.version = versionDate.format(dateFormatter);
    }

    public String getVersion() {
        return version;
    }

    public LocalDate getVersionDate() {
        return versionDate;
    }

    public static ApiVersion getFirstVersion() {
        return VERSIONS.get(0);
    }

    public static List<ApiVersion> getVersions() {
        return Collections.unmodifiableList(VERSIONS);
    }

    public static ApiVersion getDefaultVersion() {
        return VERSIONS.get(VERSIONS.size() - 1);
    }

    public static ApiVersion from(String version) {
        try {
            ApiVersion apiVersion;
            if(version.equals("1.*") || version.startsWith("1.0")) {
                apiVersion = from(Versions.V20200101);
            } else if(version.startsWith("1.1")) {
                apiVersion = from(Versions.V20200526);
            } else if(version.startsWith("1.2")) {
                apiVersion = from(Versions.V20200925);
            } else {
                LocalDate date = LocalDate.parse(version);
                apiVersion = getFirstVersion();
                for(ApiVersion current : VERSIONS) {
                    if(current.getVersionDate().isAfter(date)) {
                        break;
                    }
                    apiVersion = current;
                }
            }
            return apiVersion;
        } catch (Exception ex) {
            return getDefaultVersion();
        }
    }



}
