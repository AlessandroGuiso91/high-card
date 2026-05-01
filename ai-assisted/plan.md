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

## Tasks 5-8 (planned, outline only)

- **Task 5 — JWT security.** Spring Security + `oauth2-resource-server`. Validate policy, issuer, expiration. In-memory issuer keys for the assessment; no real IdP.
- **Task 6 — Bug fixing.** Backlog of bugs surfaced during earlier tasks but deliberately deferred here:
    - `FakeDatabase` static seeder inserts data that violates the validation rules imposed at the web boundary (phone built as `"+39" + i` → `"+390"`; names contain digits via `"First name " + i`). Symptom of a broader architectural inconsistency: the seeder bypasses the boundary entirely. Decision in scope: either remove the seeder or make it produce conformant data.
    - `AddUserAssembler.toCriteria` lastName copy-paste (`setLastName(getFirstName())`) — fixed during task 3.
    - `OrderType.BY_LASTNAME_DESC` display string — fixed during task 3.
    - `UserServiceImpl.addUser` exception swallowing — fixed during task 1.
  Re-scan for further bugs as tasks 4-5 land.
- **Task 7 — Unit tests.** JUnit 5; `MockMvc` for controllers; service tests against the in-memory repository. Coverage target: meaningful assertions on the eight tasks, not a percentage.
- **Task 8 — Javadoc + Swagger/OpenAPI.** Javadoc written contextually throughout. `springdoc-openapi-starter-webmvc-ui` to expose Swagger UI; controllers annotated with `@Operation` summaries.

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
