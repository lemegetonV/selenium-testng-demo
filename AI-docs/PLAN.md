# PLAN.md — Selenium E2E Framework Blueprint

End-to-end purchase automation against `https://demowebshop.tricentis.com`
using Java + Selenium + TestNG + Maven. This document is the locked design
reference for the build. All design decisions below are finalized unless
explicitly revisited in a later prompt.

`CLAUDE.md` stays at the project root (auto-loaded by Claude Code);
long-form artifacts like this blueprint and per-prompt narrative files
live here in `AI-docs/`.

---

## 1. Project Folder Structure

```
selenium-testng-demo/
├── .gitignore
├── pom.xml
├── testng.xml
├── README.md
├── CLAUDE.md                        # working memory, stays at project root
├── AI-docs/
│   ├── PLAN.md                      # this file — design blueprint
│   └── (one markdown file per prompt, appended chronologically)
├── reports/
│   ├── extent-report.html           # committed, latest run evidence
│   └── screenshots/                 # wiped @BeforeSuite; base64-embedded in HTML too
├── logs/
│   └── test.log                     # gitignored
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/demowebshop/
│   │   │       ├── config/
│   │   │       │   └── ConfigReader.java
│   │   │       ├── driver/
│   │   │       │   └── DriverFactory.java
│   │   │       ├── core/
│   │   │       │   ├── BasePage.java
│   │   │       │   ├── ElementActions.java
│   │   │       │   └── FrameworkException.java
│   │   │       ├── utils/
│   │   │       │   ├── WaitUtils.java
│   │   │       │   ├── ScreenshotUtils.java
│   │   │       │   ├── TestDataReader.java
│   │   │       │   └── TestDataFactory.java
│   │   │       ├── models/
│   │   │       │   ├── User.java
│   │   │       │   ├── SearchTerm.java
│   │   │       │   ├── Product.java
│   │   │       │   ├── BillingAddress.java
│   │   │       │   └── CreditCard.java
│   │   │       └── pages/
│   │   │           ├── HomePage.java
│   │   │           ├── LoginPage.java
│   │   │           ├── HeaderComponent.java
│   │   │           ├── SearchResultsPage.java
│   │   │           ├── ProductDetailsPage.java
│   │   │           ├── CartPage.java
│   │   │           ├── CheckoutPage.java
│   │   │           └── OrderConfirmationPage.java
│   │   └── resources/
│   │       └── log4j2.xml
│   └── test/
│       ├── java/
│       │   └── com/demowebshop/
│       │       ├── base/
│       │       │   └── BaseTest.java
│       │       ├── listeners/
│       │       │   └── ExtentReportListener.java
│       │       ├── dataproviders/
│       │       │   └── ProductDataProvider.java
│       │       └── tests/
│       │           └── EndToEndPurchaseTest.java
│       └── resources/
│           ├── config.properties
│           └── testdata/
│               ├── users.json
│               └── products.json
└── test-output/                     # gitignored — TestNG default; Extent supersedes
```

**Layering note.** POJOs live under `src/main/java/com/demowebshop/models/`
so `TestDataReader` (a main-tree utility) can reference them without a
test-to-main dependency inversion.

---

## 2. Maven Dependencies

**Target: Java 21** — configured via `maven-compiler-plugin` 3.13.0 with
source/target set to 21.

Versions are best-estimate stable as of **April 2026**. Verify against
Maven Central at scaffold time — any drift gets logged in `CLAUDE.md`.

| Purpose            | groupId : artifactId                           | Version  |
|--------------------|------------------------------------------------|----------|
| Browser driver     | `org.seleniumhq.selenium:selenium-java`        | 4.28.0   |
| Driver binaries    | `io.github.bonigarcia:webdrivermanager`        | 5.9.3    |
| Test runner        | `org.testng:testng`                            | 7.11.0   |
| Logging facade     | `org.slf4j:slf4j-api`                          | 2.0.16   |
| Logging impl       | `org.apache.logging.log4j:log4j-core`          | 2.24.3   |
| SLF4J → Log4j2     | `org.apache.logging.log4j:log4j-slf4j2-impl`   | 2.24.3   |
| JSON               | `com.fasterxml.jackson.core:jackson-databind`  | 2.18.2   |
| Boilerplate        | `org.projectlombok:lombok`                     | 1.18.36  |
| Fake data          | `net.datafaker:datafaker`                      | 2.4.2    |
| Reporting          | `com.aventstack:extentreports`                 | 5.1.2    |
| File utilities     | `commons-io:commons-io`                        | 2.18.0   |

**Pin-sensitive notes.**
- `log4j-core` and `log4j-slf4j2-impl` must share the same minor version or
  runtime class-loading fails silently.
- Selenium 4.28+ uses the built-in Selenium Manager; WebDriverManager is
  retained per locked design decision. Call
  `WebDriverManager.chromedriver().setup()` and stop there — do **not**
  follow it with `System.setProperty("webdriver.chrome.driver", …)`.
  Forcing the property afterward can pin Selenium to a stale binary and
  override Selenium Manager's resolution. (Amendment 7, Prompt 02.)
- Lombok requires the `annotationProcessorPaths` block in
  `maven-compiler-plugin` — not just the `<scope>provided</scope>` dep.
- `maven-surefire-plugin` 3.5.2+ for TestNG 7.11 compatibility.

---

## 3. Class Inventory

### Core utilities (`src/main/java`)

| Class              | Purpose                                                                                   |
|--------------------|-------------------------------------------------------------------------------------------|
| `DriverFactory`    | `ThreadLocal<WebDriver>` provisioning, browser + headless setup, teardown.                |
| `ConfigReader`     | Loads `config.properties`; typed getters (`getString`, `getInt`, `getBoolean`); `-D` overrides take precedence. |
| `WaitUtils`        | Thin wrapper over `WebDriverWait` — `forVisible`, `forClickable`, `forText`, `forUrlContains`. |
| `ElementActions`   | Single entry point for all DOM interactions; hosts the 3-tier click fallback and stale-retry. |
| `ScreenshotUtils`  | Captures screenshots as base64 and as files under `reports/screenshots/`.                 |
| `TestDataReader`   | Jackson-based JSON → POJO loader for files under `src/test/resources/testdata/`.          |
| `TestDataFactory`  | Datafaker-backed generator for billing/shipping/card; seedable for reproducibility.       |
| `FrameworkException` | Custom `RuntimeException` for framework-level failures with clearer stack traces.        |
| `BasePage`         | Abstract parent; exposes `actions` (ElementActions) and `wait` (WaitUtils) to subclasses. |

### POJOs (`com.demowebshop.models`)

| Class            | Fields (sketch)                                                | Source / Lombok |
|------------------|----------------------------------------------------------------|-----------------|
| `User`           | `email`, `password`                                            | JSON-loaded · `@Data @Builder @Jacksonized` |
| `SearchTerm`     | `term`, `expectedProductName`                                  | JSON-loaded · `@Data @Builder @Jacksonized` |
| `Product`        | `name`, `price`                                                | Runtime capture · `record` (no Lombok)      |
| `BillingAddress` | `firstName`, `lastName`, `email`, `country`, `state`, `city`, `address1`, `zip`, `phone` | Datafaker-generated · `@Data @Builder` |
| `CreditCard`     | `cardholder`, `cardNumber`, `cardType`, `expiryMonth`, `expiryYear`, `cvv` | Datafaker-generated · `@Data @Builder` |
| `TestCard`       | `cardType`, `cardNumber`                                       | JSON-loaded · `@Data @Builder @Jacksonized` |

**Lombok rule (Amendment 6).** Every POJO loaded from JSON gets
`@Data @Builder @Jacksonized` (Jacksonized teaches Jackson how to
deserialize through the builder). POJOs only generated by
`TestDataFactory` get `@Data @Builder` only — no `@Jacksonized` because
they are never deserialized. `Product` is a Java `record` (Amendment 2)
because it is a runtime capture of name/price scraped from the search
tile or PDP, not a config artifact.

### Page objects (`com.demowebshop.pages`)

See §4 for flow walkthrough. Summary:

| Class                  | Purpose                                                              |
|------------------------|----------------------------------------------------------------------|
| `HomePage`             | Landing after launch; `open()` (navigate to baseUrl) + title/landmark loaded-check only. Navigation is via `HeaderComponent`. |
| `LoginPage`            | Email, password, Login button; surface login error text.             |
| `HeaderComponent`      | Shared header — search box, account/logout links, cart badge. Worth a class: used at Steps 2, 3, 5, 11. |
| `SearchResultsPage`    | Product grid after search; add-to-cart directly from the tile (PDP not navigated — confirmed by exploration). |
| `CartPage`             | Line-items list, terms-of-service checkbox, Checkout button.         |
| `CheckoutPage`         | Single-page accordion. Six discrete action methods: `fillBillingAddress`, `fillShippingAddress`, `selectShippingMethod`, `selectPaymentMethod`, `fillPaymentInfo`, `confirmOrder`. |
| `OrderConfirmationPage`| Success banner + order number (extracted from container text).       |

### Test layer (`src/test/java`)

| Class                    | Purpose                                                           |
|--------------------------|-------------------------------------------------------------------|
| `BaseTest`               | `@BeforeSuite` (wipe screenshots), `@BeforeMethod` (driver + MDC), `@AfterMethod` (quit). |
| `ExtentReportListener`   | `ITestListener` — lifecycle → Extent nodes; attaches screenshots on failure. |
| `ProductDataProvider`    | `@DataProvider` reading two search terms from `products.json`.    |
| `EndToEndPurchaseTest`   | The one data-driven test covering the full 11-step flow.          |

---

## 4. Page Object Breakdown

Walking the 11-step flow and mapping each step to a page object:

| Step | Action                               | Page Object(s)                                        |
|-----:|--------------------------------------|-------------------------------------------------------|
| 1    | Launch browser → homepage            | `HomePage`                                            |
| 2    | Login                                | `HeaderComponent` → `LoginPage`                       |
| 3    | Search product                       | `HeaderComponent` (search box) → `SearchResultsPage`  |
| 4    | Add to cart from results             | `SearchResultsPage` or `ProductDetailsPage`           |
| 5    | Go to cart, verify product           | `HeaderComponent` (cart link) → `CartPage`            |
| 6    | Proceed to checkout                  | `CartPage`                                            |
| 7    | Fill billing + shipping              | `CheckoutPage` (billing + shipping sections)          |
| 8    | Shipping + payment method            | `CheckoutPage` (shipping-method + payment-method)     |
| 9    | Confirm order                        | `CheckoutPage` (confirm-order section)                |
| 10   | Verify order success                 | `OrderConfirmationPage`                               |
| 11   | Logout                               | `HeaderComponent`                                     |

**Confirmed (Prompt 03).** `CheckoutPage` is a single page object. The checkout
is a single-page accordion with six numbered sections (Billing, Shipping,
Shipping Method, Payment Method, Payment Information, Confirm Order). No URL
changes between sections.

**Confirmed (Prompt 03).** `ProductDetailsPage` is REMOVED from the class
inventory. Exploration confirmed "Add to cart" is available directly on search
result tiles for "14.1-inch Laptop" — no PDP navigation required.

**Confirmed (Prompt 03).** `HeaderComponent` is a dedicated class. It is used
across Steps 2 (login nav), 3 (search), 5 (cart nav), and 11 (logout) — more
than enough to justify a class over inlining.

**Constraint (Prompt 03 — Amendment F).** The test account has accumulated address
book entries and order history from prior candidates. Tests must verify order
success via the confirmation page only — never via order history count or address
book contents.

---

## 5. Test Data File Shapes

### `src/test/resources/testdata/users.json`

```json
{
  "defaultUser": {
    "email": "qa.user123@mailinator.com",
    "password": "Engineer@09876"
  }
}
```

`firstName` / `lastName` were dropped from the `User` POJO (Amendment 1):
the login form takes only email + password. Billing names come from
`TestDataFactory.generateBillingAddress()` per test.

### `src/test/resources/testdata/products.json`

One search term — demonstration of the DataProvider pattern. Running the full
E2E twice adds runtime without demonstrating new capability.

```json
{
  "searchTerms": [
    {
      "term": "laptop",
      "expectedProductName": "14.1-inch Laptop"
    }
  ]
}
```

### `src/test/resources/testdata/paymentCards.json`

Luhn-valid card numbers keyed by card type label (must match the payment info
dropdown exactly). Random card numbers fail Luhn checksum validation on submit.

```json
{
  "testCards": [
    { "cardType": "Visa",        "cardNumber": "4111111111111111" },
    { "cardType": "Master card", "cardNumber": "5555555555554444" }
  ]
}
```

Card type dropdown options confirmed by exploration: `Visa`, `Master card`,
`Discover`, `Amex`. The first entry (Visa) is always selected — deterministic.

Dynamic data (billing address, credit card cardholder/expiry/CVV) is **not**
stored here — it's generated per test by `TestDataFactory` using a configurable
Datafaker seed.

---

## 6. Configuration Surface

### `src/test/resources/config.properties`

```properties
# Environment
baseUrl=https://demowebshop.tricentis.com
environment=prod

# Browser
browser=chrome
headless=false

# Waits (seconds)
implicitWaitSeconds=0
explicitWaitSeconds=15
pageLoadTimeoutSeconds=30

# Test data
dataFakerSeed=42
```

The shipped `testng.xml` targets **Chrome only**. The `browser` key
remains `-D` overridable so a reviewer can swap in Firefox or Edge
locally without modifying the suite file.

**Overridable via `-D` system properties** (precedence: `-D` > file):

| Key                    | Typical override use                                 |
|------------------------|------------------------------------------------------|
| `baseUrl`              | Hit a different env.                                  |
| `browser`              | Local vs. CI browser swap.                            |
| `headless`             | CI runs headless; dev runs headful.                   |
| `environment`          | Tag reports per environment.                          |
| `dataFakerSeed`        | Reproduce a flaky run.                                |

`implicitWaitSeconds` intentionally **0** — explicit waits only, per locked
design. Implicit + explicit waits compound and cause unpredictable delays.

---

## 7. Logging Configuration

### `src/main/resources/log4j2.xml` outline

- **Appenders**
  - `Console` — `SYSTEM_OUT` with color-free pattern (CI friendly).
  - `RollingFile` — `logs/test.log`, rolls daily + at 10MB, 7-file
    history. Set `createOnDemand="true"` (Amendment 8) so the `logs/`
    directory is created on first write — no committed `.gitkeep`
    required, and the path is honored by `.gitignore`.
- **Pattern**
  ```
  %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{testName}] %logger{36} - %msg%n
  ```
  The `%X{testName}` MDC slot is populated in `BaseTest.@BeforeMethod` from
  `ITestResult.getMethod().getMethodName()`.
- **Levels**
  - Root: `INFO`
  - `com.demowebshop`: `DEBUG` (surface internals during build; tune to
    `INFO` before final commit if noise is excessive)
  - `org.openqa.selenium`: `WARN`
  - `org.apache.http`: `WARN`

---

## 8. ExtentReports Setup

### `ExtentReportListener implements ITestListener`

| TestNG hook         | Listener action                                                                          |
|---------------------|-------------------------------------------------------------------------------------------|
| `onStart`           | Init `ExtentReports` with `ExtentSparkReporter(reports/extent-report.html)`; set theme, doc title, system info (browser, baseUrl, env). |
| `onTestStart`       | Create `ExtentTest` node named from MDC `testName`; push to a `ThreadLocal<ExtentTest>`. |
| `onTestSuccess`     | `extentTest.pass("…")`.                                                                  |
| `onTestFailure`     | `ScreenshotUtils.captureBase64()` → attach via `MediaEntityBuilder`; log stack trace; also save file copy under `reports/screenshots/` for artifact pipelines. |
| `onTestSkipped`     | `extentTest.skip(reason)`.                                                               |
| `onFinish`          | `extentReports.flush()`.                                                                 |

**Report path.** Fixed: `reports/extent-report.html` (committed). Reviewers
see evidence without executing the suite.

**Screenshots.** Base64-embedded in the HTML → the file is self-contained
and shareable. A duplicate copy lands in `reports/screenshots/` for
CI artifact upload; the folder is wiped in `BaseTest.@BeforeSuite` so a
committed run is always clean.

**MDC → report.** The `testName` MDC key is both the log tag and the node
name in Extent — single source of truth for identifying a test across
logs and reports.

---

## 8a. Amendments Locked at Prompt 02

The following design refinements were locked while scaffolding the
framework. They supersede earlier mentions in this document where they
conflict.

- **DriverFactory owns Chrome configuration (Amendment 3).**
  `DriverFactory` reads `headless` (boolean) from `ConfigReader` and
  builds a `ChromeOptions` instance with a fixed set of
  noise-suppression flags: `--disable-notifications`,
  `--remote-allow-origins=*`, `--window-size=1920,1080` always;
  `--headless=new`, `--disable-gpu`, `--no-sandbox` only when headless.
  Test code does not see `ChromeOptions` directly.

- **TestNG hard assertions only (Amendment 4).** Test layer uses
  `Assert.assertEquals` / `assertTrue` / `assertNotNull`. `SoftAssert`
  is intentionally not introduced — failing fast on the first wrong
  state makes the failure cause obvious. Framework-level validation
  (timeouts, missing config keys, broken locators) throws
  `FrameworkException`, never `AssertionError`.

- **`testng.xml` is the launch contract (Amendment 5).** The suite
  file owns:
  - `parallel="none"`, `thread-count="1"` (sequential, per locked design)
  - listener registration for `ExtentReportListener`
  - suite parameters `browser` and `baseUrl` consumed by `BaseTest`
    via `@Parameters`, with `ConfigReader` as the fallback so the
    suite file and `-D` overrides remain coherent.

## 8b. Amendments Locked at Prompt 03

The following design refinements were confirmed or locked during the plan
amendment and locator exploration phase.

- **Amendment A — Checkout single-page accordion confirmed.** `CheckoutPage`
  stays as one class with six discrete action methods (see §3 page objects).

- **Amendment B — CheckoutPage six discrete action methods.**
  - `fillBillingAddress(BillingAddress)` — selects "New Address" by visible text,
    fills all fields, clicks billing Continue, waits for shipping form.
  - `fillShippingAddress(BillingAddress)` — same approach; no "ship to same address"
    shortcut exists in the DOM.
  - `selectShippingMethod(String)` — selects radio by adjacent label text,
    clicks shipping-method Continue, waits for payment-method radios.
  - `selectPaymentMethod(String)` — selects radio by adjacent label text (last label
    wins — each radio has an image label AND a text label), clicks Continue, waits
    for `#CreditCardType`.
  - `fillPaymentInfo(CreditCard)` — fills card form, clicks payment-info Continue,
    waits for Confirm button.
  - `confirmOrder()` — clicks Confirm, waits for `.section.order-completed`, returns
    `OrderConfirmationPage`.

- **Amendment C — BillingAddress `state` field added.** `faker.address().state()`
  returns full state names matching the dropdown. Country hardcoded to
  "United States" (state dropdown is country-dependent).

- **Amendment D — CreditCard uses Luhn-valid numbers from `paymentCards.json`.**
  Card number not generated randomly. `TestCard` POJO added. `generateCreditCard()`
  picks first entry deterministically. Expiry year is `LocalDate.now().getYear() + 4`
  (stays valid as calendar years advance). Dropdown confirmed: `Visa`, `Master card`,
  `Discover`, `Amex`. `paymentCards.json` values match exactly.

- **Amendment E — Shipping address always refilled independently.** No "Ship to
  same address" shortcut in DOM. `fillShippingAddress` always selects "New Address"
  and fills the form again with the same `BillingAddress` object.

- **Amendment F — Account state is dirty (constraint).** 20+ saved addresses,
  accumulated order history. Tests: (1) always select "New Address" explicitly,
  (2) verify order via confirmation banner only — never via order history.

- **Amendment G — `HomePage` stays minimal.** Only `open()` (navigate to baseUrl)
  and a loaded-check (`.header-logo` present). All navigation via `HeaderComponent`.

- **Amendment H — One search term, one product.** `products.json` contains only
  `{"term":"laptop","expectedProductName":"14.1-inch Laptop"}`. `SearchTerm.expectedKeyword`
  renamed to `expectedProductName`. `ProductDataProvider` unchanged (Jackson maps
  by field name automatically).

- **Prompt sequence renumbered.** Prompt 02 absorbed the Prompt 03 reporting scope,
  so locator exploration moved from Prompt 04 → Prompt 03. Page objects = Prompt 04,
  tests = Prompt 05, stabilize = Prompt 06, final = Prompt 07.

## 9. Prompt Sequence Ahead

Prompt 02 absorbed the reporting/listener scope originally in Prompt 03, so
the sequence is renumbered from Prompt 03 onward.

| # | Prompt                                | Goal                                                                 |
|---|---------------------------------------|----------------------------------------------------------------------|
| 1 | **Planning** (done)                   | Produce PLAN.md, CLAUDE.md, AI-docs scaffold.                        |
| 2 | Scaffold the framework (done)         | `pom.xml`, folder tree, all utility classes, `BasePage`, `BaseTest`, empty page-object stubs, `ExtentReportListener`. |
| 3 | Plan amendments + locator exploration (done) | Apply 8 design amendments; scripted Playwright exploration of the full flow; produce `AI-docs/locators.md`. |
| 4 | Build page objects                    | Implement all page objects using `AI-docs/locators.md`; all DOM interactions route through `ElementActions`. |
| 5 | Write tests                           | `EndToEndPurchaseTest` data-driven via `ProductDataProvider`; 4 assertions at the locked checkpoints. |
| 6 | Stabilize after first run             | Run the suite, triage flakiness, tune waits, refine click fallback thresholds, commit a green run. |
| 7 | Finalize README and AI-docs           | Full README (how to run, structure, CI notes); consolidate AI-docs narrative; final commit. |

---

## 10. Open Questions and Risks

- **~~Checkout DOM unknown.~~ RESOLVED (Prompt 03).** Single-page accordion
  confirmed. Six sections, no URL changes. `CheckoutPage` stays a single class.
- **~~Add-to-cart source of truth.~~ RESOLVED (Prompt 03).** Tile-level add-to-cart
  confirmed for "14.1-inch Laptop". `ProductDetailsPage` removed from inventory.
- **~~HeaderComponent scope.~~ RESOLVED (Prompt 03).** Header used at 4 steps
  (login nav, search, cart nav, logout) — class is justified.
- **Ship-to-same-address shortcut.** NOT found in DOM. Approach locked:
  always select "New Address" in shipping dropdown, refill with same
  `BillingAddress` object as billing. (Prompt 03.)
- **Billing address dropdown `New Address` option value is `""` (empty string).**
  In Selenium, use `selectByVisibleText("New Address")` rather than
  `selectByValue("")` which is ambiguous. (Prompt 03.)
- **`accountLink` not uniquely selectable.** `a[href*="customer/info"]`
  matches 2 elements (main + mobile nav). Use `.header-links a[href='/customer/info']`
  or `.first()`. Not needed for the current E2E — no impact on Prompt 04.
- **Order number extraction.** No dedicated `strong`/`li.order-number` element.
  Extracted from `.section.order-completed` container text via `split("Order number:")`.
- **`elementClickIntercepted` frequency.** Monitor at Prompt 06 run.
- **Terms-of-service checkbox.** Confirmed selector `#termsofservice`. Must tick
  before clicking `#checkout` — JS alert blocks further interaction if skipped.
- **Maven versions.** Verify at first `mvn package` run (Prompt 06). Log bumps
  in `CLAUDE.md`.
- **Account state is dirty.** Address book has 20+ saved entries from prior
  candidates. Tests must select "New Address" explicitly — never rely on the
  first dropdown entry being the correct address. Order history is also
  polluted — verify success only via the confirmation page banner, not order
  history navigation.
