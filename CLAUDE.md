# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Context

This is a **Spring Boot 3.5 / Java 17** technical assessment from Sara (it.sara). The README defines the exercise: refactoring, security fixes (SQL injection in PUT, JWT auth), validation (Italian phone numbers, email), pagination/sorting/search, centralized exception handling, bug fixes, unit tests, and Javadoc.

Submission requires a `/ai-assisted/` folder with `pre-analysis.md`, `plan.md`, and `report.md` documenting the AI-assisted workflow — not just the code result.

## Commands

```bash
./mvnw spring-boot:run                        # run app
./mvnw test                                   # run all tests
./mvnw test -Dtest=HighCardApplicationTests   # run single test class
./mvnw test -Dtest=ClassName#methodName       # run single test method
./mvnw package                                # build jar
```

## Architecture

The codebase enforces a **strict layered architecture** that must not be modified (per README). The translation between layers is done by dedicated assemblers — preserve this boundary.

```
web (controllers + Request/Response DTOs)
  └─ web.assembler  →  converts web Request → service Criteria
service (UserService, business rules)
  ├─ service.user.criteria  (input objects, layer-internal)
  ├─ service.user.result    (output objects, layer-internal)
  ├─ service.assembler      (converts DB model ↔ result)
  └─ service.database (UserRepository + FakeDatabase in-memory store)
```

Key conventions:
- **Web layer never sees `User` (DB model) and service layer never sees `*Request`/`*Response`.** Generic base types (`GenericRequest`, `GenericResponse`, `GenericCriteria`, `GenericResult`, `GenericPagedResult`) anchor each layer. The README's follow-up question explicitly asks why exposing `? extends GenericRequest/Response` into the service layer is bad — keep them out.
- **Errors flow via `GenericException` carrying a `StatusDTO` (code, message, traceId).** The exercise requires centralized exception handling (e.g. `@RestControllerAdvice`) that always returns HTTP 200 with the `StatusDTO` payload — error info goes in the body, not the HTTP status.
- **`UserServiceImpl.addUser` currently swallows the typed `GenericException`** in a broad `catch (Exception)` and rethrows `GENERIC_ERROR`, hiding the 400 validation messages. Validation errors must surface to the client; preserve specific `GenericException`s when refactoring.
- Persistence is a `FakeDatabase` static list — do not introduce JPA/JDBC. The "SQL Injection" task is conceptual: validate inputs on the PUT endpoint (currently `/user/v1/user` PUT = addUser, POST = getUsers — note the inverted REST semantics, kept for the assessment).
- Lombok is enabled via the maven-compiler-plugin annotation processor path; `@Slf4j`, `@Getter`, etc. are available.

## Notes for Working in This Repo

- The `getUsers` endpoint and service method are stubbed (`return null` / `ResponseEntity.ok().build()`) — implementing pagination/sort/search is task #3.
- `OrderType` enum referenced by README does not yet exist; create it in the service layer when implementing sorting.
- When adding JWT, keep the validation surface (policy, issuer, expiration) explicit — the README lists these as required checks.
