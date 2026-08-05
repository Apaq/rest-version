package dk.apaq.rest.version.quarkus.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import dk.apaq.rest.version.ApiVersion;
import dk.apaq.rest.version.quarkus.ApiVersionedResource;

@QuarkusTest
class RestVersionQuarkusTest {

    static {
        ApiVersion.clear();
        ApiVersion.registerVersion(new ApiVersion("2023-01-01"), false);
        ApiVersion.registerVersion(new ApiVersion("2023-02-01"), false);
        ApiVersion.registerVersion(new ApiVersion("2024-01-01"), false);
    }

    @ApiVersionedResource(version = "2023-01-01", path = "/cats")
    public static class CatsV1 {

        @GET
        public String getCats(@QueryParam("name") String name) {
            return name == null ? "v1" : "v1:" + name;
        }
    }

    @ApiVersionedResource(version = "2023-02-01", path = "/cats")
    public static class CatsV2 {

        @GET
        public String getCats() {
            return "v2";
        }
    }

    @Path("/health")
    public static class HealthResource {

        @GET
        public String health() {
            return "ok";
        }
    }

    @Test
    void exactMatch() {
        given().header("Api-Version", "2023-01-01").when().get("/cats").then().statusCode(200).body(is("v1"));
        given().header("Api-Version", "2023-02-01").when().get("/cats").then().statusCode(200).body(is("v2"));
    }

    @Test
    void fallsBackToClosestEarlierVersion() {
        given().header("Api-Version", "2023-01-15").when().get("/cats").then().statusCode(200).body(is("v1"));
        given().header("Api-Version", "2023-02-10").when().get("/cats").then().statusCode(200).body(is("v2"));
    }

    @Test
    void unknownDateServesLatestAvailableController() {
        given().header("Api-Version", "2024-01-01").when().get("/cats").then().statusCode(200).body(is("v2"));
        given().header("Api-Version", "2025-01-01").when().get("/cats").then().statusCode(200).body(is("v2"));
    }

    @Test
    void missingHeaderUsesDefaultVersion() {
        given().when().get("/cats").then().statusCode(200).body(is("v1"));
    }

    @Test
    void unparseableHeaderUsesDefaultVersion() {
        given().header("Api-Version", "not-a-date").when().get("/cats").then().statusCode(200).body(is("v1"));
    }

    @Test
    void unversionedEndpointIsUnaffected() {
        given().when().get("/health").then().statusCode(200).body(is("ok"));
        given().header("Api-Version", "2023-02-01").when().get("/health").then().statusCode(200).body(is("ok"));
    }

    @Test
    void versionedResourceIsRegisteredAtComputedPath() {
        given().when().get("/v20230201/cats").then().statusCode(200).body(is("v2"));
    }

    @Test
    void queryStringIsPreserved() {
        given().header("Api-Version", "2023-01-01").when().get("/cats?name=felix").then().statusCode(200).body(is("v1:felix"));
        given().header("Api-Version", "2023-02-01").when().get("/cats?name=felix").then().statusCode(200).body(is("v2"));
    }
}
