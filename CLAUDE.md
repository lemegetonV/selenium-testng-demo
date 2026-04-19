# CLAUDE.md — Working Memory

> **All generated code must follow the Code Style Standards section of
> this document. Re-check against those standards before completing any
> code-generation prompt.**

> **Update this file at the end of every implementation prompt. Record
> what changed, why, and any corrections from the user. Re-read this file
> at the start of every new prompt.**

**Design source of truth:** [AI-docs/PLAN.md](./AI-docs/PLAN.md). All framework decisions,
folder structure, dependencies, page object list, test data shapes,
configuration surface, logging setup, ExtentReports design, and the
remaining prompt sequence are documented there. Do not re-open those
decisions without explicit user approval — if a prompt implies a design
change, confirm with the user before acting.

---

## Progress Log

Append-only. One entry per prompt. Newest at the bottom.

### 2026-04-19 — Prompt 01: Planning

- **What was implemented**
  - Created `AI-docs/` folder (with `.gitkeep`) for prompt history —
    one markdown file per prompt, added by the user going forward.
  - Created `PLAN.md` at project root: full folder tree, Maven
    dependencies with versions, class inventory (utilities, POJOs,
    pages, tests, listeners), page-object breakdown across the 11-step
    flow, test data JSON shapes, `config.properties` surface + `-D`
    override list, `log4j2.xml` outline, `ExtentReportListener` design,
    remaining 7-prompt sequence, open questions.
  - Created `CLAUDE.md` (this file) with Progress Log, Open Items, and
    Code Style Standards.
- **Decisions made / locked** (from the prompt, not re-opened)
  - Dynamic `By` locators, not PageFactory.
  - `ThreadLocal<WebDriver>` via `DriverFactory`.
  - `ElementActions` as the only DOM entry point; 3-tier click fallback
    (native → scroll-retry → JS); DEBUG/WARN log levels accordingly;
    StaleElement triggers a single full-chain retry.
  - `BasePage` abstract with `actions` + `wait` exposed.
  - SLF4J + Log4j2; MDC `testName`; password-masking in `type()` when
    description contains "password".
  - Static data in JSON → POJOs with Lombok; dynamic data from
    Datafaker (seedable).
  - ExtentReports committed to repo at `reports/extent-report.html`;
    screenshots base64-embedded; `reports/screenshots/` wiped at
    `@BeforeSuite`.
  - Conventional commits, one commit per prompt-phase.
- **Corrections from user:** none yet.
- **Nuances / surprises:** none yet — no code written.
- **Clarifications gathered in plan mode (2026-04-19)**
  - Plan file location: `AI-docs/PLAN.md` (flat, separate from the
    per-prompt narrative files). Root `PLAN.md` was deleted; blueprint
    now lives under `AI-docs/`.
  - Browser scope: **Chrome only**. `testng.xml` ships Chrome; the
    `browser` config key still exists for `-D` override.
  - Parallel execution: **sequential only**. Simplifies Datafaker
    seeding (single global seed) and listener thread-safety.
  - Java version: **Java 21**. `maven-compiler-plugin` 3.13.0 with
    source/target 21.
- **Also added this prompt:** project `.gitignore` covering Maven
  output, TestNG default output, logs, IDE/OS cruft.
- **Next prompt:** scaffold the framework (pom.xml, folder tree,
  utility classes, empty page-object stubs).

---

## Open Items

- Verify cited Maven versions against Maven Central at scaffold time
  (Prompt 02). Log any bumps here.
- Checkout page structure (single-page accordion vs. multi-step) is
  **assumed single-page**. Confirm during Playwright codegen (Prompt 04)
  and split `CheckoutPage` if the DOM disagrees.
- Decide whether `ProductDetailsPage` is needed once we see whether
  search result tiles expose "Add to cart" directly (Prompt 05).
- Decide whether `HeaderComponent` is worth a dedicated class or should
  be inlined into the pages that use it (Prompt 05).
- Watch the click-fallback WARN rate in Prompt 07 — frequent JS
  fallbacks signal a structural locator problem, not just flakiness.
- Terms-of-service checkbox on `CartPage` must be ticked before
  checkout; skipping it yields a JS alert that's quiet to debug.

---

## Code Style Standards (applies to all generated code)

**Readability over cleverness**
- Prefer straightforward, linear code over abstractions
- No interfaces unless there are genuinely two or more implementations
- No generic type gymnastics when a concrete type works
- No design patterns applied for their own sake (no factory-of-factory, no
  abstract base classes with single concrete child)
- Method length: aim for under 20 lines; split when longer
- Class length: aim for under 200 lines; if larger, question whether it's
  doing too much

**Naming**
- Class names: nouns, intent-revealing (ElementActions, not ActionHelper)
- Method names: verbs, describe behavior (clickWithFallback, not handleClick)
- Variable names: full words, no abbreviations except well-known ones
  (url, id, db are fine; elem, btn, txt are not)
- Constants: SCREAMING_SNAKE_CASE, grouped in a Constants class only if
  shared across multiple classes

**Comments — meaningful, not noise**
- No javadoc on obvious getters/setters or self-explanatory methods
- DO add javadoc on: public utility methods, page object actions, anything
  non-obvious in behavior (e.g. the click fallback chain, the stale-retry
  logic, the password-masking logic)
- Inline comments only where the "why" isn't obvious from the code. Never
  restate what the code does ("// increment counter" above counter++).
- When a decision has a non-obvious reason (e.g. "using WARN here so
  flakiness surfaces in logs"), comment it.
- When code handles a specific edge case, comment the case.

**Formatting**
- Blank line between logical blocks inside methods
- One statement per line
- Imports organized, no wildcard imports
- Consistent Lombok usage: @Data @Builder on POJOs; @Slf4j for logging;
  don't mix Lombok and manual boilerplate in the same class

**What NOT to do**
- No "TODO" or "FIXME" comments in committed code — either do it or don't
- No commented-out code blocks
- No defensive null checks everywhere — trust the types, validate at
  boundaries only
- No try-catch that swallows exceptions silently; if catching, log and
  rethrow or handle meaningfully
- No System.out.println — always log via SLF4J
- No hardcoded waits (Thread.sleep) except as an absolute last resort with
  a comment explaining why
