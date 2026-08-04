# rest-version

Versioning support for Spring-based RESTful web services.

`rest-version` lets you serve multiple versions of the same REST API from a single
Spring Boot application. Multiple controllers can be mapped to the same path while
each one targets a different API version, making it possible to introduce breaking
changes to your API without breaking clients that still depend on older versions.

Versions are identified by an ISO date (e.g. `2023-02-01`) and clients select a
version by sending the `Api-Version` request header. Requests without a header are
routed to the default version.

## How it works

- **Register the versions** your API supports up front (at application startup).
- **Annotate controllers** with `@ApiVersionedResource(version = "yyyy-MM-dd")` so
  that each controller is bound to a specific API version.
- **Clients pick a version** via the `Api-Version` header, e.g. `Api-Version: 2023-01-01`.
- The library inspects the header, resolves it against the registered versions, and
  routes the request to the controller whose version matches.

If the requested version does not match a registered version exactly, the request is
routed to the closest earlier version. This means clients requesting `2023-02-15`
will be served by the `2023-02-01` version until a later one is released.

### Features

- Multiple controllers mapped to the same path, differentiated only by version.
- Date-based versions (`yyyy-MM-dd`) that make it obvious when a version was released.
- Client-controlled version selection via the `Api-Version` header (configurable name).
- A configurable default version (used when the header is missing or invalid).
- Graceful fallback to the closest earlier registered version for unknown dates.
- Versioning at both the controller (class) level and the method level, where a
  method-level version overrides the version declared on its controller.
- Automatic Spring Boot configuration — no manual bean wiring required.
- Fails fast at startup if a controller references a version that was never registered.

## Requirements

- Java 17+
- Spring Boot 3.x (Spring MVC)

## Installation

Add the dependency to your build. The library is published to GitHub Packages.

### Maven

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/Apaq/rest-version</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>dk.apaq</groupId>
        <artifactId>rest-version</artifactId>
        <version>1.0.3-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### Gradle

```groovy
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Apaq/rest-version")
    }
}

dependencies {
    implementation "dk.apaq:rest-version:1.0.3-SNAPSHOT"
}
```

> Note: publishing to GitHub Packages requires authentication. See
> [GitHub's documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
> for how to configure a token.

## Getting started

### 1. Register the supported versions

Register every API version your application supports, before the web context is built
(e.g. in your application's `main` method or a `@Configuration` class).

The first registered version becomes the default. Use `registerVersion(version, true)`
or `ApiVersion.setDefaultVersion(version)` to pick a different default.

```java
import dk.apaq.rest.version.ApiVersion;

ApiVersion.registerVersion(new ApiVersion("2023-01-01"));
ApiVersion.registerVersion(new ApiVersion("2023-02-01"));
ApiVersion.registerVersion(new ApiVersion("2024-01-01"));
```

### 2. Create versioned controllers

Annotate each controller with `@ApiVersionedResource(version = "...")` and map the
same path on as many controllers as you have versions.

In the example below, a request with `Api-Version: 2023-01-01` is handled by the old
controller, while `Api-Version: 2023-02-01` is handled by the new one.

__Old version__

```java
import dk.apaq.rest.version.ApiVersionedResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ApiVersionedResource(version = "2023-01-01")
public class CatController {

    @GetMapping("/cats")
    public List<CatV1> getCats() {
        // ...
    }
}
```

__New version__

```java
import dk.apaq.rest.version.ApiVersionedResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ApiVersionedResource(version = "2023-02-01")
public class CatController {

    @GetMapping("/cats")
    public List<CatV2> getCats() {
        // ...
    }
}
```

### 3. Configure your application

Add the required property to `application.properties` so the version-aware handler
mapping can replace Spring Boot's default:

```properties
spring.main.allow-bean-definition-overriding=true
```

When `rest-version` is on the classpath, its auto-configuration registers
`ApiVersionedRequestMapping` automatically (you can disable it with
`rest-version.enabled=false`, or register your own `ApiVersionedRequestMapping` bean).

### 4. Call the API

```console
$ curl -H "Api-Version: 2023-02-01" https://api.example.com/cats
```

Omitting the header routes the request to the default version:

```console
$ curl https://api.example.com/cats
```

## Method-level versioning

An individual method can override the version declared on its controller. Because
`@ApiVersionedResource` is meta-annotated with `@RequestMapping`, declare the mapping
through the annotation's own `path` and `method` attributes rather than combining it
with `@GetMapping` (Spring does not support multiple `@RequestMapping` annotations on
one method).

```java
@RestController
@ApiVersionedResource(version = "2023-01-01")
public class CatsController {

    @ApiVersionedResource(version = "2023-02-01", path = "/cats/featured", method = RequestMethod.GET)
    public List<Cat> getFeaturedCats() {
        // ...
    }
}
```

A method-level version applies only to that endpoint: with `Api-Version: 2023-01-01`
the endpoint above is not available, while `Api-Version: 2023-02-01` (or later) serves
it.

## Configuring the version header

By default the version is read from the `Api-Version` header. To use a different header,
register your own handler mapping and set the header name:

```java
@Configuration
public class VersionedApiConfig {

    @Bean
    @ConditionalOnMissingBean(ApiVersionedRequestMapping.class)
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        ApiVersionedRequestMapping mapping = new ApiVersionedRequestMapping();
        mapping.setHeaderName("X-API-Version");
        return mapping;
    }
}
```

## Version resolution

When a request arrives, the version is resolved as follows:

1. The configured version header is read. If it is missing or empty, the **default version**
   is used (the first registered version, or the one selected explicitly).
2. The header value is parsed as an ISO date and matched against the registered versions.
3. If an exact match exists, that version is used. Otherwise the **closest earlier**
   registered version is used (e.g. `2023-02-15` falls back to `2023-02-01`).
4. If the header cannot be parsed, the default version is used.

The request is then routed to the controller whose version is the latest one that is
equal to or earlier than the resolved version.

### Registration rules

- Versions must be registered before the web context starts.
- Registering the same version twice throws an `IllegalArgumentException`.
- Referencing a version in `@ApiVersionedResource` that was never registered fails fast
  at startup with a clear error.

## API reference

| Type | Description |
| ---- | ----------- |
| `ApiVersion` | Represents an API version and the registry of supported versions. Construct from a `String` (`yyyy-MM-dd`), a `LocalDate`, or `year, month, day`. |
| `@ApiVersionedResource(version = "...", path = "", method = {})` | Annotation that binds a controller or method to a specific API version. |
| `ApiVersionedRequestMapping` | Custom `RequestMappingHandlerMapping` that applies version conditions. |
| `ApiVersionedResourceAutoConfiguration` | Spring Boot auto-configuration that registers the handler mapping. |
| `ApiVersionedResourceRequestCondition` | Internal request condition that matches a request against a version based on the version header. |

### Key `ApiVersion` methods

| Method | Description |
| ------ | ----------- |
| `registerVersion(ApiVersion)` | Registers a supported version. |
| `registerVersion(ApiVersion, boolean defaultVersion)` | Registers a version and optionally makes it the default. |
| `setDefaultVersion(ApiVersion)` | Sets the default version explicitly. |
| `getVersions()` | Returns all registered versions. |
| `getDefaultVersion()` | Returns the current default version. |
| `getFirstVersion()` | Returns the first registered version. |
| `from(String version)` | Resolves a version string to the closest matching registered version. |
| `clear()` | Clears all registered versions (mainly useful for tests). |

## License

[MIT](LICENSE) © Apaq
