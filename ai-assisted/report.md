# Final Report — AI-Assisted Engineering Workflow

> An assessment of the AI-assisted workflow utilized for this project.
> Tools used: **Claude Code** (CLI agent) for implementation, **IntelliJ AI Assistant** for commit generation.

---

## 1. Executive Summary
This report details the successful delivery of the technical assessment using an AI-assisted workflow. The project was completed in approximately **8 focused hours** across 3 sessions—an estimated **40–50% reduction in delivery time**, net of time lost to incorrect AI suggestions and rework, compared to a traditional manual approach (approx. 15 hours).

The AI acted as a powerful accelerator for boilerplate generation and syntax reference. This allowed me to shift my cognitive load entirely onto higher-value tasks: architectural design, security boundaries, testing strategies, and code quality. The result is a more robust, fully documented deliverable that includes "nice-to-have" features (CI pipeline, Swagger) that would typically be deprioritized under real-world delivery constraints.

---

## 2. Workflow Strategy & Prompting

My approach treated the AI as a high-speed implementation assistant and reference tool, not as an architect. The strategy focused on iterative feedback rather than exhaustive upfront prompting.

* **Iterative Decomposition:** Instead of requesting full feature implementations, I forced the AI to break tasks down (e.g., *"Filter -> Sort -> Paginate -> Wire"*). This kept code reviews manageable and prevented compounding errors.
* **Explicit Decision Elicitation:** For complex features like JWT security, I prompted the AI to present trade-offs (e.g., *"Self-contained issuer vs. external Keycloak?"*). I evaluated the options and made the final architectural calls.
* **Strict Ownership Boundaries:** I established firm rules early on: Git operations, bug attribution, and business-logic validation were exclusively my responsibility. The AI proposed; I executed.

---

## 3. Handling AI Limitations & Challenges

Documenting the failure modes highlights the necessity of human oversight. The following challenges required active engineering intervention:

* **Tooling Blindspots:** The AI confidently proposed an inefficient CI dependency scanner (OWASP) that timed out due to rate limits. I discarded it and implemented a modern, native alternative (`actions/dependency-review-action`). *Lesson: AI optimizes for the most-cited solution, not the best contextual fit.*
* **Confident Inaccuracies:** When asked if `OrderType` existed in the codebase, the AI confidently stated it didn't, missing a nested enum. *Lesson: A confident negative from an LLM requires careful validation. Manual code inspection remains mandatory.*
* **Version Incompatibilities:** The AI suggested outdated `springdoc-openapi` versions, leading to conflict spirals with Spring Boot 3.5. I had to pause the agent, manually cross-check compatibility matrices, and enforce the correct dependency.
* **Contextual Blindness:** The AI operates without UI or environmental context. It spent cycles debugging a JWT converter when the actual issue was a missing `Authorization` header in my local HTTP client—a diagnostic gap that required human environmental awareness to bridge.

---

## 4. Impact & Value Produced

Using AI did not reduce the complexity of the problem space; it reduced only the cost of implementation. This gain, however, is conditional on strict validation discipline; without it, the same workflow can easily degrade into compounding errors.

* **Time Reinvested:** The hours saved on typing Regex patterns, Spring Security boilerplate, and DTO mapping were reinvested into code quality.
* **Contextual Documentation:** Design decisions (`pre-analysis.md`, `plan.md`) were drafted continuously alongside the code, rather than as an afterthought at the end of the project.
* **Cleaner Architecture:** Because rewriting code via AI is cheap, I was more willing to undertake structural refactorings (e.g., splitting `SecurityConfig` into smaller, single-responsibility classes) that I might have skipped manually to save time.

---

## 5. Net Assessment & Key Takeaways

The dynamic was highly productive: I designed the system and validated the logic; the AI wrote the bulk of the syntax. For an assessment aimed at demonstrating AI collaboration, this workflow highlights how tooling can elevate an engineer's output when properly managed.

**Key Takeaways:**
1. **AI is for velocity, the Human is for direction:** The AI excels at boilerplate and search. The engineer remains indispensable for architectural decisions, security validation, and evaluating trade-offs.
2. **Engineering discipline outweighs prompting skill:** Structured workflows, validation loops, and incremental delivery matter more than prompt sophistication.
3. **Assumption Verification is Mandatory:** You cannot outsource accountability. Every critical path, from dependency choices to authorization logic, requires strict human validation against AI hallucinations.

Ultimately, the correctness of the system, the validity of architectural decisions, and the accountability for delivery quality remained entirely my responsibility. This workflow demonstrates that AI can significantly increase delivery speed and documentation quality—provided that architectural control and validation remain human-owned.

In this model, AI is not a substitute for engineering expertise—it is a multiplier of it.