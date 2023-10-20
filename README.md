# rest-patch

This library provides versioning for requests in Spring-based RESTFUL webservices. 

## What does it do?

This project adds support for versioning Spring MVC resources so that multiple controllers for the same paths can be 
served for different versions of the API. It makes it possible to make breaking changes to a REST api, while still 
supporting clients that uses the old version of the API.

The version must be an ISO date (fx. '2022-12-31'). The version must be specified via the header `Api-Version`.

### Example
The following examples show how to have 2 controllers mapped to the same path return different models based on the version
specified in the `Api-Version` header. If the header `Api-Version: 2023-01-01` is specified the old controller will be
handling the request. If the header `Api-Version: 2023-02-01` is specified, then the new controller will handle it instead.

__Old version__

```Java
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@ApiVersionedResource(version = "2023-01-01")
public class CatController {

    @GetMapping("/cats")
    public List<CatV1> getCats() { ... }
}
```

__New version__
```Java
@RestController
@ApiVersionedResource(version = "2023-02-01")
public class CatController {

    @GetMapping("/cats")
    public List<CatV2> getCats() { ... }
}
```

