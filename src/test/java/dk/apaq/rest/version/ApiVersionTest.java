package dk.apaq.rest.version;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiVersionTest {

    @BeforeEach
    void setup() {
        // Clear versions and reset default version before each test
        ApiVersion.clear();
    }

    @Test
    void testApiVersionCreation() {
        ApiVersion version = new ApiVersion("2024-09-10");
        assertEquals("2024-09-10", version.getVersion());
        assertEquals(LocalDate.of(2024, 9, 10), version.getVersionDate());
    }

    @Test
    void testRegisterVersion() {
        ApiVersion version1 = new ApiVersion("2023-01-01");
        ApiVersion version2 = new ApiVersion("2024-01-01");

        ApiVersion.registerVersion(version1, false);
        ApiVersion.registerVersion(version2, true);

        assertEquals(2, ApiVersion.getVersions().size());
        assertEquals(version1, ApiVersion.getFirstVersion());
        assertEquals(version2, ApiVersion.getDefaultVersion());
    }

    @Test
    void testGetFirstVersion_noVersionRegistered() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, ApiVersion::getFirstVersion);
        assertEquals("No versions registered", exception.getMessage());
    }

    @Test
    void testGetDefaultVersion() {
        ApiVersion version1 = new ApiVersion("2022-05-10");
        ApiVersion version2 = new ApiVersion("2023-05-10");

        ApiVersion.registerVersion(version1, false);
        ApiVersion.registerVersion(version2, false);

        assertEquals(version1, ApiVersion.getDefaultVersion()); // First registered version becomes default
    }

    @Test
    void testFromExactMatch() {
        ApiVersion version1 = new ApiVersion("2023-01-01");
        ApiVersion version2 = new ApiVersion("2023-06-01");

        ApiVersion.registerVersion(version1, false);
        ApiVersion.registerVersion(version2, false);

        ApiVersion result = ApiVersion.from("2023-06-01");
        assertEquals(version2, result);
    }

    @Test
    void testFromClosestEarlierVersion() {
        ApiVersion version1 = new ApiVersion("2023-01-01");
        ApiVersion version2 = new ApiVersion("2023-06-01");

        ApiVersion.registerVersion(version1, false);
        ApiVersion.registerVersion(version2, false);

        ApiVersion result = ApiVersion.from("2023-04-01");
        assertEquals(version1, result); // Closest earlier version is returned
    }

    @Test
    void testFromNoMatch() {
        ApiVersion version1 = new ApiVersion("2022-01-01");
        ApiVersion version2 = new ApiVersion("2023-01-01");

        ApiVersion.registerVersion(version1, false);
        ApiVersion.registerVersion(version2, false);

        ApiVersion result = ApiVersion.from("2021-01-01");
        assertEquals(version1, result); // Default version returned as no match found
    }

    @Test
    void testGetVersions() {
        ApiVersion version1 = new ApiVersion("2021-12-01");
        ApiVersion version2 = new ApiVersion("2022-06-01");

        ApiVersion.registerVersion(version1, false);
        ApiVersion.registerVersion(version2, false);

        List<ApiVersion> versions = ApiVersion.getVersions();
        assertEquals(2, versions.size());
        assertEquals("2021-12-01", versions.get(0).getVersion());
        assertEquals("2022-06-01", versions.get(1).getVersion());
    }
}
