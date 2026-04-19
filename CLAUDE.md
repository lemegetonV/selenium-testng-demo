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

### 2026-04-19 — Prompt 02: Framework Scaffold

- **What was implemented**
  - `pom.xml` — Java 21, `maven-compiler-plugin` 3.13.0,
    `maven-surefire-plugin` 3.5.2, all PLAN.md §2 dependencies, Lombok
    `annotationProcessorPaths` block.
  - `testng.xml` — `parallel="none"`, `thread-count="1"`, listener
    registration for `ExtentReportListener`, suite parameters `browser`
    and `baseUrl`.
  - `.gitignore` — verified; covers `target/`, `test-output/`, `logs/`,
    IDE/OS cruft. `reports/` intentionally not ignored.
  - **core/** — `FrameworkException`, `BasePage`, `ElementActions` (full
    3-tier click fallback + stale retry + password masking + 10 methods).
  - **config/** — `ConfigReader` (static init, typed getters,
    `System.getProperty` wins over file).
  - **driver/** — `DriverFactory` (ThreadLocal, WebDriverManager.setup(),
    ChromeOptions with headless/noise-suppression flags).
  - **utils/** — `WaitUtils`, `ScreenshotUtils`, `TestDataReader`,
    `TestDataFactory` (seeded Datafaker, US format).
  - **models/** — `User`, `SearchTerm` (`@Jacksonized`); `Product`
    (Java record); `BillingAddress`, `CreditCard` (`@Builder` only).
  - **pages/** — 8 page-object stubs: `HomePage`, `LoginPage`,
    `HeaderComponent`, `SearchResultsPage`, `ProductDetailsPage`,
    `CartPage`, `CheckoutPage`, `OrderConfirmationPage`.
  - **base/** — `BaseTest` (`@BeforeSuite` wipes screenshots, `@Parameters`
    reads suite params, `@BeforeMethod` inits driver + MDC,
    `@AfterMethod` quits + clears MDC).
  - **listeners/** — `ExtentReportListener` (full `ITestListener`; base64
    screenshot on failure via `MediaEntityBuilder`; flush on finish).
  - **dataproviders/** — `ProductDataProvider` (`@DataProvider
    "searchTerms"`).
  - **tests/** — `EndToEndPurchaseTest` (stub `@Test`, replaced Prompt 06).
  - **resources** — `config.properties`, `users.json`, `products.json`,
    `log4j2.xml` (`createOnDemand="true"` on RollingFile, MDC slot).
  - **reports/** — `.gitkeep` + `screenshots/.gitkeep`.

- **Plan Amendments applied**
  1. **User POJO** — dropped `firstName`/`lastName`. `email` + `password`
     only. `users.json` updated accordingly.
  2. **Product** — Java `record(String name, String price)` in
     `com.demowebshop.models`. Not loaded from JSON; populated at runtime.
  3. **DriverFactory ChromeOptions** — reads `headless` from ConfigReader;
     always-on flags: `--disable-notifications`, `--remote-allow-origins=*`,
     `--window-size=1920,1080`; headless-only: `--headless=new`,
     `--disable-gpu`, `--no-sandbox`.
  4. **Hard assertions only** — no `SoftAssert` introduced; framework
     failures throw `FrameworkException`. Added to PLAN.md §8a.
  5. **testng.xml suite parameters** — `browser` and `baseUrl` as suite
     params; `BaseTest` reads via `@Parameters` with ConfigReader fallback.
  6. **Jacksonized rule** — `@Jacksonized` on `User` and `SearchTerm` only;
     `BillingAddress` and `CreditCard` use `@Builder` only (never
     deserialized). Explicit note added to PLAN.md §3.
  7. **WebDriverManager** — removed the erroneous
     `System.setProperty("webdriver.chrome.driver", …)` call from
     PLAN.md §2. `setup()` only.
  8. **createOnDemand** — `RollingFile` appender in `log4j2.xml` uses
     `createOnDemand="true"` so `logs/` is created on first write; no
     committed `.gitkeep` needed.

- **Maven version drift vs. PLAN.md §2**
  - All versions used verbatim from PLAN.md (knowledge cutoff Jan 2026).
    Maven Central live-check was not performed during scaffold; deviations,
    if any, will surface as resolution errors on first `mvn package`.
    Log actual versions here once verified.

- **Nuances / discoveries during scaffold**
  - `ElementClickInterceptedException` is a subclass of
    `ElementNotInteractableException` in Selenium 4. Catching both in the
    same `multi-catch` block causes a compile error. Resolved by catching
    only `ElementNotInteractableException` (covers both). No behavior
    change.
  - `@Jacksonized` requires the class already has `@Builder`. It works
    with Lombok's immutable builders, which is why POJOs use `final`
    fields — no mutable setter needed. `BillingAddress` and `CreditCard`
    omit it correctly because they are never deserialized.
  - `TestDataFactory.generatePhoneNumber()` generates a raw 10-digit long
    cast to string rather than using Datafaker's phone provider, which
    returns formatted strings (e.g. `555-555-5555`) that demowebshop's
    field may reject. Noted in code comment; confirm at Prompt 07 during
    first real run.

- **Open Items resolved this prompt**
  - Maven version-verification open item deferred to first `mvn package`
    run (Prompt 07).

- **New open items**
  - `TestDataFactory.generatePhoneNumber()` format assumption — verify
    demowebshop accepts a raw 10-digit numeric string at Prompt 07.
  - `ExtentReportListener.onFinish()` calls `testNode.remove()` but each
    `onTestStart` pushes a new node — in sequential mode this is fine;
    confirm no leak if the test class is re-used across multiple methods
    in a future prompt.

- **Corrections from user:** none (prompt 02 auto-execution).

- **Next prompt:** Playwright codegen to capture locators for the 11-step
  flow and produce a structured locators map per page (Prompt 04 per
  PLAN.md §9 — Prompt 03 "reporting polish" is already fully wired in
  this scaffold, so we can proceed directly to locator discovery).

---

### 2026-04-19 — Prompt 03: Plan Amendments + Locator Exploration

- **What was implemented**
  - Applied 8 plan amendments (A–H) to PLAN.md, source files, and test data.
  - Created `exploration/` directory with `package.json` (Playwright 1.59.1)
    and `explore.js` — a scripted headless Playwright runner that walks the
    full 11-step flow, evaluates selector strategies, captures dropdown values,
    and takes numbered screenshots.
  - Ran explore.js against the live site; full flow completed successfully
    (order placed, order confirmed, logout verified).
  - Generated `AI-docs/locators.md` — per-page locator tables with By expressions,
    dropdown/radio value lists, parameterized templates, and exploration notes.
  - 23 screenshots saved to `exploration/screenshots/` (01-homepage.png →
    23-logged-out.png).

- **Plan Amendments applied**
  - **A** — Checkout single-page accordion confirmed; CheckoutPage remains one class.
  - **B** — CheckoutPage gets six discrete action methods (fill billing/shipping,
    select shipping method, select payment method, fill payment info, confirm order).
  - **C** — `BillingAddress` gets `state` field; `generateBillingAddress()` uses
    `faker.address().state()`; country hardcoded to "United States".
  - **D** — `TestCard` POJO created; `paymentCards.json` created with Luhn-valid
    Visa (4111111111111111) and Master card (5555555555554444) numbers.
    `generateCreditCard()` updated: picks first card deterministically; expiry year
    dynamic (`now().getYear() + 4`). Dropdown confirmed: Visa, Master card, Discover, Amex.
  - **E** — Shipping address always refilled independently; no "Ship to same address"
    shortcut found in DOM. Approach locked: select "New Address" in shipping dropdown,
    refill with same `BillingAddress` object.
  - **F** — Account state dirty constraint documented: always select "New Address",
    verify success via confirmation page only.
  - **G** — `HomePage` minimal: `open()` + `.header-logo` loaded-check. All nav via
    `HeaderComponent`.
  - **H** — `products.json` updated to one term: `laptop` / `14.1-inch Laptop`.
    `SearchTerm.expectedKeyword` renamed to `expectedProductName`. `ProductDataProvider`
    unchanged (Jackson maps by field name).
  - **Prompt sequence renumbered**: Prompt 02 absorbed Prompt 03 scope, so locator
    exploration is now Prompt 03. Page objects = 04, tests = 05, stabilize = 06,
    final = 07. PLAN.md §9 updated.

- **Exploration outcome**
  - Flow: complete (exit 0, all 11 steps walked, order placed).
  - Screenshot count: 23 (numbered 01–23 matching flow order).
  - Script iterations needed: 5 (fixed `/new/i` regex matching "New York" instead of
    "New Address"; fixed `-block` waitForSelector race; fixed payment radio label
    strict-mode violation; fixed confirm-order race with `Promise.all`).

- **Key discoveries**
  - **Add-to-cart path: TILE.** `input[value="Add to cart"]` is on the search result
    tile. `ProductDetailsPage` is **unused** — removed from PLAN.md §3.
  - **Ship-to-same-address shortcut: NOT FOUND.** Always use "New Address" + refill.
  - **Account state dirty.** Billing/shipping dropdowns have 20+ saved addresses.
    Must select "New Address" by visible text (its option value is empty string `""`).
  - **Payment method labels.** Each radio has two `<label>` elements (image + text).
    Use `.last()` in Playwright / text-based XPath in Selenium to get the display name.
  - **Order number.** No `li.order-number` or `strong` — plain text in
    `.section.order-completed`. Extract with `split("Order number: ")[1].trim()`.
  - **Confirmation page navigation.** `waitForURL` races with redirect; use
    `Promise.all([waitForSelector('.section.order-completed'), click(confirm)])`.
  - **Notification bar** captures successfully: `#bar-notification`,
    text = "The product has been added to your shopping cart".
  - **Payment method "Credit Card"** label confirmed (not "Credit card" or "CREDIT CARD").

- **Dropdown option strings captured**
  - Billing address book: 20 saved addresses + "New Address" (option value `""`)
  - Billing country (first 10): Select country, United States, Canada, Afghanistan, …
  - Billing state (US): 62 options (Alabama, Alaska, …, Wyoming) + Armed Forces codes
  - Shipping address book: same 20 + "New Address" + newly added Jane Tester entry
  - Shipping methods: "Ground (0.00)", "Next Day Air (0.00)", "2nd Day Air (0.00)"
  - Payment methods: "Cash On Delivery (COD) (7.00)", "Check / Money Order (5.00)",
    "Credit Card", "Purchase Order"
  - Credit card type: "Visa", "Master card", "Discover", "Amex"
  - Expiry month: "01"–"12"
  - Expiry year: "2026"–"2040"

- **Brittle selectors flagged for Prompt 04**
  - `input[name="shippingoption"]` and `input[name="paymentmethod"]` are group
    selectors (count > 1). In Selenium, get all elements and filter by adjacent
    label text — don't try to use a single unique selector.
  - `#billing-address-select` / `#shipping-address-select` — always select by
    visible text "New Address", not by value (value is `""`).
  - Payment section `-block` container IDs are always in DOM but hidden; don't
    waitForVisible on them — wait for the specific interaction element inside.

- **Corrections from user:** none (auto-execution).

- **Next prompt:** Prompt 04 — implement all page objects using `AI-docs/locators.md`
  as the source of truth.

---

### 2026-04-19 — Prompt 04: Page Object Implementation

- **Files modified**
  - `src/main/java/com/demowebshop/core/ElementActions.java` — added
    `findAllTexts(By, String) List<String>` and `check(By, String)` methods.
  - `src/main/java/com/demowebshop/pages/HomePage.java` — implemented `open()`,
    `isLoaded()`.
  - `src/main/java/com/demowebshop/pages/HeaderComponent.java` — implemented
    `clickLoginLink()`, `searchFor()`, `openCart()`, `clickLogout()`,
    `isLoggedIn()`, `getCartItemCount()`.
  - `src/main/java/com/demowebshop/pages/LoginPage.java` — implemented
    `login(User)`, `isLoaded()`, `getErrorMessage()`.
  - `src/main/java/com/demowebshop/pages/SearchResultsPage.java` — implemented
    `isLoaded()`, `containsProduct()`, `addProductToCart()`.
  - `src/main/java/com/demowebshop/pages/CartPage.java` — implemented
    `isLoaded()`, `getCartItemNames()`, `acceptTermsOfService()`,
    `proceedToCheckout()`, `checkout()`.
  - `src/main/java/com/demowebshop/pages/CheckoutPage.java` — implemented all
    six accordion methods: `fillBillingAddress()`, `fillShippingAddress()`,
    `selectShippingMethod()`, `selectPaymentMethod()`, `fillPaymentInfo()`,
    `confirmOrder()`.
  - `src/main/java/com/demowebshop/pages/OrderConfirmationPage.java` —
    implemented `isLoaded()`, `isOrderPlaced()`, `getOrderNumber()`,
    `continueShopping()`.
  - `pom.xml` — upgraded Lombok from 1.18.36 → **1.18.38** to fix Java 21.0.10
    `TypeTag.UNKNOWN` annotation processor crash. Added `<fork>true</fork>` and
    eight `-J--add-opens` `compilerArgs` for `jdk.compiler` internals.
  - `.mvn/jvm.config` — created with `--add-opens` directives (belt-and-suspenders;
    the pom.xml fork approach is the effective fix).

- **Locators.md updates** — none required. All locators used verbatim from
  `locators.md`. Two XPath templates (shipping/payment radio selection) already
  existed as a documented pattern in locators.md; they were promoted to named
  constants in CheckoutPage.

- **Nuances / discoveries**
  - **Lombok 1.18.36 + Java 21.0.10 compile crash** — `mvn clean compile` failed
    with `NoSuchFieldException: TypeTag :: UNKNOWN` even in the Prompt 02 scaffold.
    This was a pre-existing issue. Fixed by upgrading Lombok to 1.18.38.
  - **`wait.forClickable(STATE_DROPDOWN)` between country and state selection** —
    added as a minimal guard to give the AJAX-populated state dropdown a moment
    to load. If it still flakes in Prompt 06, replace with a custom
    `WebDriverWait.until` that polls for a non-default state option.
  - **Order number regex** — used `Pattern.compile("Order number:\\s*(\\d+)")` to
    extract the numeric ID from the `.section.order-completed` container text.
    More precise than a split approach.
  - **`getCartItemCount()` returns 0 on empty cart** — handles both "Shopping cart"
    (no parens) and "Shopping cart(N)" (with count). Parsing tolerates unexpected
    formats.

- **Open items resolved**
  - `selectPaymentMethod` radio label approach: implemented as XPath
    `//input[@name='paymentmethod'][following-sibling::label[contains(...)]]`.
  - `selectShippingMethod` same XPath pattern.
  - "New Address" selected via `selectByVisibleText` — not `selectByValue`.
  - Order number extracted via regex on container text.
  - Terms-of-service handled by `ElementActions.check()` (idempotent).

- **New open items for Prompt 05/06**
  - State dropdown AJAX wait (`wait.forClickable`) may still race in headless CI.
    Flag for Prompt 06 stabilization if observed.
  - `addProductToCart` uses the full 15-second explicit wait for the
    notification bar. If the bar fades too quickly and the wait misses it,
    a shorter-timeout WaitUtils variant will be needed in Prompt 06.
  - Confirm that `mvn clean compile` produces a clean build after the Lombok
    upgrade (verified: BUILD SUCCESS with 23 source files).

- **Corrections from user:** none (auto-execution).

- **Next prompt:** Prompt 05 — write the EndToEndPurchaseTest using the page objects.

---

## Open Items

- Maven version drift (if any) surfaces at first `mvn package` run (Prompt 06).
- **Account state dirty constraint** — always select "New Address" explicitly in
  billing and shipping dropdowns. Verify order success via confirmation banner
  only — never via order history count or address book contents.
- State dropdown AJAX wait in `fillBillingAddress`/`fillShippingAddress` may race
  in headless CI — tune with a proper `until` poll in Prompt 06 if flaky.
- Watch the click-fallback WARN rate at Prompt 06 — frequent JS fallbacks signal a
  structural locator problem.
- `TestDataFactory.generatePhoneNumber()` format: confirm demowebshop accepts raw
  10-digit numeric string at Prompt 06.
- `ExtentReportListener.onFinish()` calls `testNode.remove()` — verify no ThreadLocal
  leak if multiple `@Test` methods run in the same class (Prompt 06/07).

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
