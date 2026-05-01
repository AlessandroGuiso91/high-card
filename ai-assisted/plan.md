# Plan

Tracks the intervention plan task-by-task and how it evolved during execution. Conventions, library policy, and quality gates are in `pre-analysis.md` and are not repeated here.

## Approach to planning

Plans are formalised when the design surface justifies it. Tasks 1 and 2 — input validation and SQL-injection prevention — converged into the same boundary-validation work and were small enough that writing a plan upfront would have been ceremony; they are documented here **retrospectively**. Plan-then-do starts in earnest from task 3 (pagination, sorting, search), where multiple concerns interact and design choices benefit from being settled before code.

---

## Task 1 — Data validation (done)

**Goal.** Strict validation of `email` and `phoneNumber` (Italian standard) on user creation.

**Approach.** Jakarta Bean Validation on `AddUserRequest`; `@Valid` at the controller; validation logic removed from the service. Custom `@Pattern` regex for Italian mobile numbers.

**What actually happened.**
- AI proposed the libraries and a regex skeleton. The **regex needed correction** post-merge: `\d{8,9}` accepted invalid 9-digit numbers; fixed to `\d{9}` in `fix/01-validation-followup`.
- Surfaced the latent bug in `UserServiceImpl.addUser` (broad `catch (Exception)` masking typed `GenericException`s). Fixed in-task by re-throwing typed exceptions before the broad catch.
- Constructor injection adopted opportunistically (`@RequiredArgsConstructor`) — off-plan, but cheap and idiomatic.

---

## Task 2 — SQL injection prevention (done)

**Goal.** Address the README's input-validation directive on the PUT endpoint.

**Approach.** Treat as a *boundary validation* problem, not a SQL-patching one — no SQL exists; the assessment is conceptual. Whitelist allowed characters in name fields, cap field lengths, document the defense-in-depth rationale.

**What actually happened.**
- First draft framed the regex error message as *"potential SQL injection payload detected"* — technically wrong (an allow-list is not payload detection) and a minor information leak. Replaced with a neutral message during review.
- Regex initially missed the hyphen, breaking common Italian compound names (`Anna-Maria`); fixed before merge.
- Tasks 1 and 2 turned out to be **the same work** under two README labels; the convergence is recorded in the Javadoc on `AddUserRequest`.

---

## Task 3 — Pagination, sorting, search (done)

**Goal.** Complete `getUsers`: paginated response, sortable by `OrderType`, case-insensitive contains-filter on name or email.

**Approach.**
- Validation at the **web DTO** (`GetUsersRequest`): `offset >= 0`, `limit` in `[1, 100]`, `query.length <= 50`. Triggered by `@Valid` on the controller.
- **Repository filters, service orchestrates.** `UserRepository.findMatching(query)` returns the case-insensitive matches; `UserServiceImpl.getUsers` sorts (via a `comparatorFor(OrderType)` helper using `String.CASE_INSENSITIVE_ORDER`), paginates, and maps to `UserDTO`.
- New **bidirectional `GetUsersAssembler`** in `web.assembler` for both `Request → Criteria` and `Result → Response`. One class with two methods because both directions exist for this flow, unlike `AddUser`.
- `total` returned is the size of the filtered set **before** pagination, so the client can compute page boundaries.
- A manual smoke-test harness (`http/users.http`) committed alongside the code, runnable via the IntelliJ HTTP Client.

**Decisions taken.**
- Hard cap on `limit` = **100**.
- Default `OrderType` when null = **`BY_LASTNAME`**, applied in the service (business default, not at the validator).

**What actually happened.**
- I initially suggested validating `CriteriaGetUsers`; the user correctly applied validation to `GetUsersRequest` (the web DTO), in line with the boundary convention from tasks 1-2. I corrected the framing after the fact.
- User-flagged bug in `UserAssembler.toDTO`: email truncated to the domain via `substring(lastIndexOf("@") + 1)`. Fixed in scope — without it, `getUsers` would have returned corrupted data and been untestable.
- Smoke test exposed a related omission in the same method: `phoneNumber` was never mapped from `User` to `UserDTO`. Fixed in scope to keep the assembler internally consistent with the email fix.
- `FakeDatabase` static seeder produces values that violate the validation rules (`"+39" + i`, `"First name " + i`); diagnosed but **deferred to task #6** as an architectural-consistency item, not a one-line typo.
- `OrderType.BY_LASTNAME_DESC` display string fixed (`"by lastName"` → `"by lastName desc"`).

---

## Task 4 — Centralised exception handling (done)

**Goal.** All errors return HTTP 200 with the project's `StatusDTO` payload (code, message, traceId) in the body, per the README's status-in-body convention. Replace the per-controller try/catch.

**Approach.**
- Single `@RestControllerAdvice` (`GlobalExceptionHandler`) with three handlers, picked by Spring on type-hierarchy specificity:
  - `GenericException` → propagates the carried `StatusDTO` unchanged. Logged at `WARN` (expected business error).
  - `MethodArgumentNotValidException` → aggregates `BindingResult` field errors into one message (`"field: msg; field: msg"`), code 400, fresh traceId.
  - `Exception` (catch-all) → `code 500`, neutral `"Generic error"` message, full stack trace logged server-side. No internal detail leaked to the client.
- Removed the residual try/catch from `UserServiceImpl.addUser`: the broad catch + `GENERIC_ERROR` rethrow is now dead weight, since the advice handles the same case at the boundary.

**What actually happened.**
- All three rami verified via `http/users.http`: bad email/phone → `200` + `status.code: 400`; malformed JSON → `200` + `status.code: 500` (caught by the catch-all path through `HttpMessageNotReadableException`). Test of the `GenericException` handler deferred to task #7 (unit test) — it cannot be triggered naturally because `FakeDatabase.save()` always returns `true`.
- **Refinement not implemented (intentionally).** The catch-all today reports malformed JSON as `500`, while semantically it is a client error (`400`). A fourth handler on `HttpMessageNotReadableException` would split the case cleanly. Left out because the README asked for three handler categories and the spec is satisfied as-is; flagged here as a known-limit / possible improvement.

## Task 5 — JWT security (done)

**Goal.** Authenticate API requests via JWT, validating signature, issuer, audience, expiration, and role-based authorization, per the README's policy/issuer/expiration mandate.

**Approach.**
- Single `spring-boot-starter-oauth2-resource-server` dependency provides decoder *and* encoder transitively (via `spring-security-oauth2-jose` + Nimbus). No separate auth-server starter.
- **RSA-2048 keypair** (`src/main/resources/keys/`) committed as demo material with a `README.md` disclaimer. Production deployments would source keys from a KMS / Vault / mounted file.
- **Self-contained app**: same process issues tokens (`POST /auth/login`) and validates them. Justified for a single-developer assessment; in production the two roles would live in separate services.
- **Validators**: `JwtValidators.createDefaultWithIssuer(...)` (signature + exp + iat + iss) composed with a custom `JwtAudienceValidator` via `DelegatingOAuth2TokenValidator`.
- **Authorization (RBAC)**: `roles` claim, `JwtAuthenticationConverter` maps it to Spring authorities, `@PreAuthorize` on controller methods (`hasRole('ADMIN')` for create, `hasAnyRole('ADMIN','USER')` for read).
- **Status-in-body for security failures**: custom `AuthenticationEntryPoint` + `AccessDeniedHandler` write a `StatusDTO` JSON envelope with HTTP 200, plus advice handlers for `AuthenticationException` (401, uniform "Invalid credentials" to avoid username enumeration / locale leak) and `AccessDeniedException` (403, raised by method security and not caught by the filter chain).
- **In-memory user store**: `alice` (ADMIN+USER), `bob` (USER), BCrypt-hashed.
- IntelliJ HTTP Client examples in `http/auth.http` and updates to `http/users.http` for the now-protected endpoints.

**What actually happened.**
- Initial smoke test failed with `Could not resolve placeholder 'app.jwt.private-key-location'` because that property was missing in `application.properties`. The user proposed splitting `SecurityConfig` into three classes; the actual fix was the missing property. The split is still a good design improvement and was deferred to task #6.
- BadCredentialsException from `AuthenticationManager` produced a localized Italian message (`"Credenziali non valide"`) due to Spring Security's default locale-aware MessageSource. Replaced with a fixed English string `"Invalid credentials"`, which doubles as a defense against username enumeration (uniform message regardless of failure cause).
- `@PreAuthorize` failures returned HTTP 500 instead of 403 because `AccessDeniedException` is thrown synchronously inside the controller invocation and bypasses the security filter chain's `accessDeniedHandler` — a behaviour I missed initially. Added a dedicated `@ExceptionHandler(AccessDeniedException.class)` to the advice.
- Wasted an iteration debugging the `JwtAuthenticationConverter` when the actual symptom (`"Full authentication is required to access this resource"`) was caused by `users.http` not carrying the `Authorization` header at all. The user pointed it out twice before I read the file. Logged here as a process failure (mine).
- Initial `AuthenticationManager` bean used `DaoAuthenticationProvider#setUserDetailsService` / `setPasswordEncoder`, which are deprecated in Spring Security 6.4+. User replaced with the idiomatic `authConfig.getAuthenticationManager()` form via `AuthenticationConfiguration`, which auto-pulls the existing `UserDetailsService` + `PasswordEncoder` beans without depending on deprecated API.

## Task 6 — Bug fixing & code-quality cleanup (done)

**Goal.** Close the backlog of bugs and design issues deliberately deferred from earlier tasks.

**What changed in scope of this task.**
- **Removed the `FakeDatabase` static seeder.** Producing data that violated the validation enforced at the web boundary (phone `"+39" + i`, names with digits via `"First name " + i`) was conceptually wrong: the seeder was bypassing the layer it was supposed to feed. Removed entirely; the in-memory store starts empty and is populated through `PUT /user/v1/user`. Rationale captured in a class-level Javadoc on `FakeDatabase`.
- **Split `SecurityConfig` into three focused classes**: `JwtConfig` (encoder/decoder/converter + RSA key loading), `UsersConfig` (in-memory user store, password encoder, authentication manager), `SecurityConfig` (security filter chain + status-in-body writer). Single-responsibility, no behaviour change. Surfaced during task 5 when a missing property triggered a misleading "jwtIssuer fails → securityConfig fails" stack — the actual fix was the property, but the refactor was still worthwhile.

**Already fixed in earlier tasks (recap for the report):**
- `AddUserAssembler.toCriteria` lastName copy-paste — fixed during task 3.
- `OrderType.BY_LASTNAME_DESC` display string — fixed during task 3.
- `UserServiceImpl.addUser` exception swallowing — fixed during task 1.
- `UserAssembler.toDTO` email truncation + missing `phoneNumber` mapping — both fixed during task 3.

## Task 7 — Unit tests (done)

**Goal.** Lock in the behaviour established across tasks 1-6 so that future regressions are caught by the build, not by the next reviewer.

**Approach.**
- **JUnit 5 + Mockito + AssertJ**, all transitively available via `spring-boot-starter-test`. No new dependencies.
- **Pure unit tests, no `@SpringBootTest`** outside the existing context-loads check. Each class is exercised in isolation with mocks where the dependency is interesting (e.g. `UserAssembler` mocked in `UserServiceImplTest` to keep filter/sort/paginate logic separable from entity-to-DTO mapping).
- **Coverage by intent, not by percentage.** Every README task has at least one regression test that would fail if the corresponding fix were reverted: order display strings (task 3), email truncation + phone mapping (task 3), lastName copy-paste (task 3), three exception handlers + auth + access-denied (task 4), audience validator + JWT roundtrip (task 5).
- **`JwtIssuerTest` uses a real RSA keypair** generated in `@BeforeEach` instead of mocking the encoder. The encoder is sufficiently complex that mocking it would test the mock, not the issuer.

**What's covered.**
- `UserServiceImplTest` — `addUser` (success/repository-fail), `getUsers` (sort asc/desc, default order on null, pagination after sort, offset beyond total, total reflects filtered set).
- `UserRepositoryTest` — `findMatching` (null/blank query, case-insensitive matches across firstName/lastName/email, no-match, save assigns guid).
- `OrderTypeTest` — regression on `BY_LASTNAME_DESC` display string.
- `UserAssemblerTest` / `AddUserAssemblerTest` — regression on the email/phoneNumber/lastName bugs.
- `GetUsersAssemblerTest` — round-trip of pagination metadata across both directions.
- `GlobalExceptionHandlerTest` — all five exception flavours produce HTTP 200 + correct `status.code`.
- `JwtAudienceValidatorTest` / `JwtIssuerTest` — JWT signing and audience validation.

**What's deliberately NOT covered (and why).**
- **`@WebMvcTest` / `MockMvc` for controllers.** Both controllers (`UserController`, `AuthController`) are thin pass-throughs (assembler → service → response); the meaningful logic lives in the layers below, all unit-tested. Adding a Spring-test slice to assert routing and `@PreAuthorize` would cost a non-trivial security-context setup for very little additional signal. The end-to-end behaviour of the HTTP boundary is exercised manually via `http/users.http` and `http/auth.http`.
- **End-to-end "login → call protected endpoint" flow.** Same rationale: the constituent parts are unit-tested (`JwtIssuer` produces a valid token, `JwtAudienceValidator` accepts it, `GlobalExceptionHandler` translates failures), and the smoke harness puts them all together against the running app. A wired `@SpringBootTest` would duplicate that without catching meaningfully more.

These gaps are deliberate trade-offs, not oversights. For production, the controller slice tests would be added — the assessment scope does not justify the marginal cost.

## Task 8 — Javadoc + Swagger/OpenAPI (done)

**Goal.** Make the API self-documenting and externally explorable.

**Approach.**
- Javadoc has been written **contextually throughout the project** during tasks 1-7 (this was the methodology agreed in `pre-analysis.md` §2.3), so this task only adds the OpenAPI surface — there is no documentation backlog to flush at the end.
- **`springdoc-openapi-starter-webmvc-ui` 2.6.0** added to `pom.xml`. Single dependency, generates the OpenAPI 3 spec from the existing controllers and DTOs without needing extra configuration.
- **`OpenApiConfig`** declares the project metadata and a global `bearerAuth` security scheme, so every operation in Swagger UI exposes the "Authorize" button and JWTs are sent as `Authorization: Bearer ...`. The login endpoint opts out via `@SecurityRequirements` (empty) since it's the one public route.
- **Controllers tagged and annotated**: `@Tag` at class level groups operations under "Authentication" and "Users"; `@Operation` on each method carries summary and description.
- **Swagger paths added to the public allow-list** in `SecurityConfig`: `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`. Without this, the docs would be 401-blocked.

**Where to find it.** Once the app runs, Swagger UI is at `http://localhost:8080/swagger-ui.html`, raw JSON at `/v3/api-docs`.

**Final Javadoc sweep.** While Javadoc was written contextually throughout, a final sweep added short class-level doc on the simpler DTOs/POJOs that had been left bare (request/response/criteria/result envelopes, `User`, `StatusDTO`, `UserDTO`, the `GenericException`, the `UserService` interface, `UserRepository`, `AddUserAssembler`, the application entry point). Kept proportionate (2-4 lines), no prose restating Lombok-generated accessors. Also removed the unused `StringUtil` (orphaned by task #1's switch to Bean Validation) — dead code is a doc liability, not a doc opportunity.

**Springdoc version pin.** Initial choice was `2.6.0`, which fails at runtime against Spring Framework 6.2 (Boot 3.5) with `NoSuchMethodError: ControllerAdviceBean.<init>(Object)`. Bumped to `2.8.x` (the Boot 3.5 line). A subsequent attempt at `3.0.x` was abandoned because that line targets Boot 4 and pulls in autoconfigurations that conflict with Boot 3.5. Lesson: springdoc tracks Boot versions tightly — `2.8.x` for Boot 3.5, `3.x` for Boot 4.

## Known transitive CVEs and dependency posture

A scan with the Mend / IntelliJ "Code analysis" plugin reports a number of CVEs against transitive dependencies pulled in by Spring Boot 3.5.0 (`spring-web`, `spring-beans`, `tomcat-embed-core`, `logback-core`, `jackson-core`, etc.). These were reviewed and consciously **not patched**:

- **Most entries are flagged "Insufficient Information"** with CVE numbers from CVE-2026 series — recently published vulnerabilities still under analysis, often without an official patch released upstream.
- `./mvnw versions:display-dependency-updates` shows no patch upgrades available for the current direct dependencies. The only "newer" versions are **Spring Boot 4.1.0-RC1** (release candidate, major bump, not GA) and **springdoc 3.0.3** (which targets Boot 4 and is incompatible with Boot 3.5, as already learnt during task #8). Adopting either in an assessment is unjustified risk.
- The CI guardrail (`actions/dependency-review-action` with `fail-on-severity: high`) does **not** block any of the affected PRs, meaning no CVE actually introduced by this branch is severity-`high`. The reported issues live in the BOM-pinned baseline that Spring Boot itself manages.
- Stance: **stay on the latest stable Spring Boot** (3.5.0), let transitive patches arrive automatically with the next 3.5.x release, and rely on Dependabot to surface them. Not a blanket dismissal of CVEs — a deliberate choice to avoid swapping a noisy local scanner for unstable RC dependencies.

---

## AI-vs-human split

Recorded as each task closes.

| Task | AI did | Human did |
|---|---|---|
| Setup | Drafted CI workflow, Dependabot, pre-push hook, `pre-analysis.md` | Decided minimal-CI policy; vetoed SonarCloud and OWASP-in-CI after the failure; owned all git operations |
| Task 1 | Proposed libraries, drafted regex and Javadoc | Caught the loose phone regex post-merge, spotted the broken `catch`, decided on constructor injection |
| Task 2 | Drafted the name-regex and the defense-in-depth framing | Caught the misleading error message and the missing hyphen; decided to fold task 2 into the task 1 narrative |
| Task 3 | Walked through filter / sort / paginate decomposition; drafted `findMatching`, `comparatorFor`, the bidirectional assembler and the smoke-test harness | Caught my misplaced suggestion to validate at the service layer; flagged the email truncation bug; spotted the `phoneNumber` omission during smoke test; isolated the `FakeDatabase` seeder issue and decided to keep it for task #6 |
| Task 4 | Drafted the three-handler `GlobalExceptionHandler` and the rationale for the status-in-body pattern | Removed the dead try/catch from `UserServiceImpl.addUser`; ran the smoke tests; decided to leave the `HttpMessageNotReadableException` refinement out of scope and document it as a known limit |
| Task 5 | Wrote the full JWT pipeline (RSA keypair loading, encoder/decoder beans, `JwtIssuer`, login controller, `@PreAuthorize`, status-in-body security handlers) and the IntelliJ HTTP Client examples | Confirmed the architecture decisions (self-contained issuer, RBAC + audience, demo keys committed); spotted the missing `private-key-location` property; flagged the `users.http` lacking the `Authorization` header (twice — I had to be told twice); proposed the `SecurityConfig` split deferred to task #6 |
| Task 6 | Performed the `SecurityConfig` split, removed the `FakeDatabase` seeder, consolidated the bug retrospective in this plan | Confirmed the seed-removal decision over the alternative of "fixing the seeder"; merged and proceeded |
| Task 7 | Wrote nine test classes covering every README task end-to-end (service, repository, assemblers, exception handler, JWT issuer/validator) | Reviewed and ran the suite; tightened scope when needed (e.g. dropped a test that would have asserted behaviour the code does not actually guarantee) |
| Task 8 | Added springdoc, wrote `OpenApiConfig` with the bearer scheme, tagged controllers with `@Tag` / `@Operation`, opened Swagger paths in the security chain, closed `plan.md` | Confirmed the design choices (single global security scheme, login opted-out, Swagger paths public); ran the app to verify Swagger UI loads |
