package dk.apaq.rest.version;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ApiVersionedResourceRequestConditionTest {

    private ApiVersionedResourceRequestCondition conditionV1;
    private ApiVersionedResourceRequestCondition conditionV2;
    private ApiVersion v1;
    private ApiVersion v2;

    @BeforeEach
    void setUp() {
        v1 = new ApiVersion(LocalDate.of(2023, 1, 1));
        v2 = new ApiVersion(LocalDate.of(2024, 1, 1));
        ApiVersion.registerVersion(v1, false);
        ApiVersion.registerVersion(v2, true);

        conditionV1 = new ApiVersionedResourceRequestCondition(Collections.singleton(v1));
        conditionV2 = new ApiVersionedResourceRequestCondition(Collections.singleton(v2));
    }

    @Test
    void testCombineConditions() {
        // Combine conditionV1 and conditionV2
        ApiVersionedResourceRequestCondition combinedCondition = conditionV1.combine(conditionV2);

        // Verify that the combined condition contains both versions
        Set<ApiVersion> combinedVersions = (Set<ApiVersion>) combinedCondition.getContent();
        assertEquals(2, combinedVersions.size());
        assertTrue(combinedVersions.contains(v1));
        assertTrue(combinedVersions.contains(v2));
    }

    @Test
    void testGetMatchingCondition_Match() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(ApiVersionedResourceRequestCondition.HEADER_VERSION)).thenReturn("2023-01-01");

        ApiVersionedResourceRequestCondition matchingCondition = conditionV1.getMatchingCondition(request);

        // Verify that the matching condition is found for version 2023-01-01
        assertNotNull(matchingCondition);
    }

    @Test
    void testGetLatestVersion() {
        // Verify that the latest version is correctly identified
        ApiVersion latestVersion = conditionV2.getLatestVersion();
        assertEquals(v2, latestVersion);
    }

    @Test
    void testCompareTo() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // Verify that v2 is newer than v1
        assertTrue(conditionV2.compareTo(conditionV1, request) < 0);

        // Verify that v1 is older than v2
        assertTrue(conditionV1.compareTo(conditionV2, request) > 0);
    }

    @Test
    void testGetDefaultVersion_WhenHeaderMissing() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader(ApiVersionedResourceRequestCondition.HEADER_VERSION)).thenReturn(null);

        ApiVersionedResourceRequestCondition defaultCondition = conditionV1.getMatchingCondition(request);

        // Verify that default version is used when no version header is provided
        assertNotNull(defaultCondition);
    }
}
