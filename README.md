# rest-version

Versioning support for RESTful web services built with **Spring Boot** or **Quarkus**.

`rest-version` lets you serve multiple versions of the same REST API from a single
application. Multiple controllers can be mapped to the same path while each one targets a
different API version, making it possible to introduce breaking changes to your API without
breaking clients that still depend on older versions.

Versions are identified by an ISO date (e.g. `2023-02-01`) and clients select a version by
sending the `Api-Version` request header. Requests without a header are routed to the
default version.

## Modules

| Module | Description |
| ------ | ----------- |
| `rest-version` | The Spring Boot / Spring MVC integration. |
| `rest-version-core` | Framework-agnostic version registry shared by both integrations. |
| `rest-version-quarkus` | Quarkus extension for Quarkus REST (Jakarta REST). |

## How it works

- **Register the versions** your API supports up front (at application startup).
- **Annotate controllers** with `@ApiVersionedResource(version = "yyyy-MM-dd")` so that each
  controller is bound to a specific API version.
- **Clients pick a version** via the `Api-Version` header, e.g. `Api-Version: 2023-01-01`.
- The library inspects the header, resolves it against the registered versions, and routes
  the request to the controller whose version matches.

If the requested version does not match a registered version exactly, the request is routed
to the closest earlier version. This means clients requesting `2023-02-15` will be served by
the `2023-02-01` version until a later one is released.

### Features

- Multiple controllers mapped to the same path, differentiated only by version.
- Date-based versions (`yyyy-MM-dd`) that make it obvious when a version was released.
- Client-controlled version selection via the `Api-Version` header (configurable name).
- A configurable default version (used when the header is missing or invalid).
- Graceful fallback to the closest earlier registered version for unknown dates.
- Fails with a clear error if a controller references a version that was never registered.

---

## Spring Boot

### Requirements

- Java 17+
- Spring Boot 3.x (Spring MVC)

### Installation

Add the dependency to your build. The library is published to GitHub Packages.

#### Maven

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

#### Gradle

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

### Getting started

#### 1. Register the supported versions

Register every API version your application supports, before the web context is built (e.g.
in your application's `main` method or a `@Configuration` class).

The first registered version becomes the default. Use `registerVersion(version, true)` or
`ApiVersion.setDefaultVersion(version)` to pick a different default.

```java
import dk.apaq.rest.version.ApiVersion;

ApiVersion.registerVersion(new ApiVersion("2023-01-01"));
ApiVersion.registerVersion(new ApiVersion("2023-02-01"));
ApiVersion.registerVersion(new ApiVersion("2024-01-01"));
```

#### 2. Create versioned controllers

Annotate each controller with `@ApiVersionedResource(version = "...")` and map the same path
on as many controllers as you have versions.

```java
import dk.apaq.rest.version.ApiVersionedResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ApiVersionedResource(version = "2023-01-01")
public class CatController {

    @GetMapping("/cats")
    public List<Cat> getCats() {
        // ...
    }
}
```

```java
@RestController
@ApiVersionedResource(version = "2023-02-01")
public class CatController {

    @GetMapping("/cats")
    public List<Cat> getCats() {
        // ...
    }
}
```

#### 3. Configure your application

Add the required property to `application.properties` so the version-aware handler mapping
can replace Spring Boot's default:

```properties
spring.main.allow-bean-definition-overriding=true
```

When `rest-version` is on the classpath, its auto-configuration registers
`ApiVersionedRequestMapping` automatically (you can disable it with
`rest-version.enabled=false`, or register your own `ApiVersionedRequestMapping` bean).

#### 4. Call the API

```console
$ curl -H "Api-Version: 2023-02-01" https://api.example.com/cats
```

Omitting the header routes the request to the default version.

### Method-level versioning

An individual method can override the version declared on its controller. Because
`@ApiVersionedResource` is meta-annotated with `@RequestMapping`, declare the mapping through
the annotation's own `path` and `method` attributes rather than combining it with
`@GetMapping` (Spring does not support multiple `@RequestMapping` annotations on one method).

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

A method-level version applies only to that endpoint: with `Api-Version: 2023-01-01` the
endpoint above is not available, while `Api-Version: 2023-02-01` (or later) serves it.

### Configuring the version header

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

### Version resolution

When a request arrives, the version is resolved as follows:

1. The configured version header is read. If it is missing or empty, the **default version**
   is used (the first registered version, or the one selected explicitly).
2. The header value is parsed as an ISO date and matched against the registered versions.
3. If an exact match exists, that version is used. Otherwise the **closest earlier**
   registered version is used (e.g. `2023-02-15` falls back to `2023-02-01`).
4. If the header cannot be parsed, the default version is used.

The request is then routed to the controller whose version is the latest one that is equal to
or earlier than the resolved version.

### Registration rules

- Versions must be registered before the web context starts.
- Registering the same version twice throws an `IllegalArgumentException`.
- Referencing a version in `@ApiVersionedResource` that was never registered fails fast at
  startup with a clear error.

---

## Quarkus

The Quarkus integration is a proper Quarkus extension built on top of Quarkus REST
(RESTEasy Reactive). It is designed to work with native image.

### Requirements

- Java 17+
- Quarkus 3.17+
- `quarkus-rest` (RESTEasy Reactive)

### Installation

#### Maven

```xml
<dependency>
    <groupId>dk.apaq</groupId>
    <artifactId>rest-version-quarkus</artifactId>
    <version>1.0.4-SNAPSHOT</version>
</dependency>
```

#### Gradle

```groovy
implementation "dk.apaq:rest-version-quarkus:1.0.4-SNAPSHOT"
```

The deployment artifact `rest-version-quarkus-deployment` is resolved automatically from the
extension descriptor, so you only need to add the runtime dependency.

### Getting started

#### 1. Register the supported versions

Register every API version your application supports before the first request, e.g. in a
`@Startup` bean or in `main` before `Quarkus.run(args)`:

```java
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import dk.apaq.rest.version.ApiVersion;

@ApplicationScoped
public class VersionRegistration {

    @PostConstruct
    void registerVersions() {
        ApiVersion.registerVersion(new ApiVersion("2023-01-01"));
        ApiVersion.registerVersion(new ApiVersion("2023-02-01"));
        ApiVersion.registerVersion(new ApiVersion("2024-01-01"));
    }
}
```

#### 2. Create versioned controllers

Annotate each controller with `@ApiVersionedResource(version = "...", path = "/cats")`. The
extension registers the class as a Jakarta REST resource under the versioned path
`/v<yyyyMMdd><path>` — the version is only ever written once. Use regular Jakarta REST method
annotations (`@GET`, `@POST`, ...) on the methods.

```java
import jakarta.ws.rs.GET;

import dk.apaq.rest.version.quarkus.ApiVersionedResource;

@ApiVersionedResource(version = "2023-01-01", path = "/cats")
public class CatControllerV1 {

    @GET
    public List<Cat> getCats() {
        // ...
    }
}
```

```java
@ApiVersionedResource(version = "2023-02-01", path = "/cats")
public class CatControllerV2 {

    @GET
    public List<Cat> getCats() {
        // ...
    }
}
```

Clients keep calling `/cats`; the extension rewrites the request to the versioned path based
on the `Api-Version` header.

#### 3. Call the API

```console
$ curl -H "Api-Version: 2023-02-01" https://api.example.com/cats
```

### How it works

- At build time the extension scans for `@ApiVersionedResource` classes and registers each one
  as a Jakarta REST resource under `/v<yyyyMMdd><path>`.
- A pre-matching request filter reads the `Api-Version` header, resolves it against the
  registered versions (with the same closest-earlier fallback as Spring), and rewrites the
  request to the matching versioned path.
- Unversioned resources are untouched.

### Configuration

| Property | Default | Description |
| -------- | ------- | ----------- |
| `quarkus.rest-version.header-name` | `Api-Version` | The request header that carries the API version. |
| `quarkus.rest-version.enabled` | `true` | Whether version routing is enabled. |

### Differences from the Spring Boot integration

- Controllers use **Jakarta REST annotations** (`@GET`, `@POST`, ...) instead of Spring MVC
  annotations.
- The `path` is declared on `@ApiVersionedResource` itself; do **not** add `@Path` to a
  versioned controller.
- **Method-level versioning is not supported.** JAX-RS routes per class, so each version
  needs its own controller. Using `@ApiVersionedResource` on a method fails the build.
- Versions must be registered before the **first request** (the extension validates this and
  fails fast with a clear error if a referenced version was never registered).

---

## API reference

| Type | Description |
| ---- | ----------- |
| `ApiVersion` | Represents an API version and the registry of supported versions (shared by both integrations). Construct from a `String` (`yyyy-MM-dd`), a `LocalDate`, or `year, month, day`. |
| `@ApiVersionedResource(version = "...", path = "...")` | Spring: binds a controller or method to a specific API version. Quarkus: binds a controller to a version and declares its logical path. |
| `ApiVersionedRequestMapping` | Spring: custom `RequestMappingHandlerMapping` that applies version conditions. |
| `ApiVersionedResourceAutoConfiguration` | Spring: auto-configuration that registers the handler mapping. |
| `ApiVersionedResourceRequestCondition` | Spring: internal request condition that matches a request against a version. |
| `dk.apaq.rest.version.quarkus.ApiVersionedResource` | Quarkus: annotation that marks a Jakarta REST controller as versioned. |
| `dk.apaq.rest.version.quarkus.VersioningRequestFilter` | Quarkus: pre-matching filter that routes requests to the correct version. |

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

## Building

```console
$ ./mvnw verify
```

This builds all modules and runs the Spring and Quarkus test suites. The Quarkus extension
is designed for native image; to verify a native build locally, run the Quarkus tests with
`-Dnative` from the `rest-version-quarkus-deployment` module.

## License

[MIT](LICENSE) © Apaq
