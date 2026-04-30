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

## Task 3 — Pagination, sorting, search (current)

**Goal.** Complete `getUsers`: paginated response, sortable by `OrderType`, case-insensitive contains-filter on a search string (name or email).

**Approach.**
- Bean Validation on `CriteriaGetUsers`: `offset >= 0`, `limit` in `[1, 100]`. Reject invalid input early.
- Filtering in `UserRepository`: stream the in-memory list, lowercase `contains` over `firstName + lastName + email`, sort by `OrderType`, then paginate (`skip` / `limit`).
- Cross-layer translation through the existing assemblers — no `User` in the web layer, no `*Response` in the service.
- Fix `OrderType.BY_LASTNAME_DESC` display string (`"by lastName"` → `"by lastName desc"`) — bug spotted during pre-analysis.

**Open decisions.**
- Hard cap on `limit`? Proposed: **100**. Defends the in-memory store from accidental abuse without adding configuration.
- Default `OrderType` when none is provided? Proposed: **`BY_LASTNAME`** (most common UX expectation).

---

## Tasks 4-8 (planned, outline only)

- **Task 4 — Centralised exception handling.** `@RestControllerAdvice` translating `GenericException` and `MethodArgumentNotValidException` into a `StatusDTO` body, always HTTP 200. Removes the residual try/catch in `UserServiceImpl`.
- **Task 5 — JWT security.** Spring Security + `oauth2-resource-server`. Validate policy, issuer, expiration. In-memory issuer keys for the assessment; no real IdP.
- **Task 6 — Bug fixing.** `OrderType.BY_LASTNAME_DESC` display (rolled into task 3); `UserServiceImpl.addUser` exception swallowing (already fixed in task 1). Re-scan for further bugs as tasks 3-5 land.
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
