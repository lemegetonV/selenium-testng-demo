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
  retained per locked design decision, but ensure no conflicting driver
  downloads trigger — handle with explicit
  `System.setProperty("webdriver.chrome.driver", …)` after
  `WebDriverManager.chromedriver().setup()`.
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

| Class            | Fields (sketch)                                                |
|------------------|----------------------------------------------------------------|
| `User`           | `email`, `password`, `firstName`, `lastName`                   |
| `SearchTerm`     | `term`, `expectedKeyword`                                      |
| `Product`        | `name`, `price` (optional — for cart assertion)                |
| `BillingAddress` | `firstName`, `lastName`, `email`, `country`, `city`, `address1`, `zip`, `phone` |
| `CreditCard`     | `cardholder`, `cardNumber`, `cardType`, `expiryMonth`, `expiryYear`, `cvv` |

All POJOs: `@Data @Builder @Jacksonized` where needed.

### Page objects (`com.demowebshop.pages`)

See §4 for flow walkthrough. Summary:

| Class                  | Purpose                                                              |
|------------------------|----------------------------------------------------------------------|
| `HomePage`             | Landing after launch; provides `open()` and entry affordances.       |
| `LoginPage`            | Email, password, Login button; surface login error text.             |
| `HeaderComponent`      | Shared header — search box, account/logout links, cart badge.        |
| `SearchResultsPage`    | Product grid after search; add-to-cart from tile OR navigate to PDP. |
| `ProductDetailsPage`   | PDP — quantity, add-to-cart. Included as safety net (see §10).       |
| `CartPage`             | Line-items list, terms-of-service checkbox, Checkout button.         |
| `CheckoutPage`         | Billing / shipping / shipping-method / payment-method / confirm.     |
| `OrderConfirmationPage`| Success banner + order number.                                       |

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

**Assumption (flagged).** `CheckoutPage` is modeled as a single page object
because demowebshop's checkout is a single-page accordion. Confirmed only
after Prompt 04 (Playwright codegen + DOM inspection). If accordion sections
behave as independent pages or have their own URLs, split into
`BillingAddressStep`, `ShippingAddressStep`, `ShippingMethodStep`,
`PaymentMethodStep`, `PaymentInfoStep`, `ConfirmOrderStep`.

**Assumption (flagged).** `ProductDetailsPage` may be redundant if "Add to
cart" buttons render directly on search result tiles. Keep it in the
inventory as a safety net; delete if unused after Prompt 05.

**Assumption (flagged).** `HeaderComponent` is a component, not a full
page. Modeled as a plain class extending `BasePage` but named `*Component`
for clarity. If the nav surface is trivial, we inline into the pages that
use it and drop this class.

---

## 5. Test Data File Shapes

### `src/test/resources/testdata/users.json`

```json
{
  "defaultUser": {
    "email": "qa.user123@mailinator.com",
    "password": "Engineer@09876",
    "firstName": "QA",
    "lastName": "User"
  }
}
```

### `src/test/resources/testdata/products.json`

```json
{
  "searchTerms": [
    {
      "term": "computer",
      "expectedKeyword": "Computer"
    },
    {
      "term": "book",
      "expectedKeyword": "Book"
    }
  ]
}
```

Dynamic data (billing address, credit card) is **not** stored here — it's
generated per test by `TestDataFactory` using a configurable Datafaker seed.

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
  - `RollingFile` — `logs/test.log`, rolls daily + at 10MB, 7-file history.
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

## 9. Prompt Sequence Ahead

| # | Prompt                                | Goal                                                                 |
|---|---------------------------------------|----------------------------------------------------------------------|
| 1 | **Planning** (this prompt)            | Produce PLAN.md, CLAUDE.md, AI-docs scaffold.                        |
| 2 | Scaffold the framework                | `pom.xml`, folder tree, all utility classes, `BasePage`, `BaseTest`, empty page-object stubs. |
| 3 | Add reporting and listeners           | `ExtentReportListener`, `log4j2.xml`, screenshot capture wiring, MDC. |
| 4 | Playwright CLI codegen → locators map | Run `playwright codegen` against the 11-step flow; translate selectors to a structured locators map per page; document the mapping.  |
| 5 | Build page objects                    | Implement the page objects (and components) using the locators map; all interactions route through `ElementActions`. |
| 6 | Write tests                           | `EndToEndPurchaseTest` data-driven via `ProductDataProvider`; 4 assertions at the locked checkpoints. |
| 7 | Stabilize after first run             | Run the suite, triage flakiness, tune waits, refine click fallback thresholds, commit a green run. |
| 8 | Finalize README and AI-docs           | Full README (how to run, structure, CI notes); consolidate AI-docs narrative; final commit. |

---

## 10. Open Questions and Risks

- **Checkout DOM unknown.** The single-page-accordion assumption may be
  wrong. Risk: significant rework of `CheckoutPage` into multiple step
  classes. Resolved at Prompt 04.
- **Add-to-cart source of truth.** Unclear whether search tiles expose
  "Add to cart" directly or funnel through PDP. Risk: `ProductDetailsPage`
  is orphaned or mandatory — decide at Prompt 05.
- **HeaderComponent scope.** Header content (search, cart badge, logout)
  may differ between guest and logged-in states. Risk: conditional
  locators. Addressed at Prompt 04/05.
- **`elementClickIntercepted` frequency.** The 3-tier click fallback is
  the safety net, but if we hit the JS fallback on every click, something
  is structurally wrong (overlay, sticky header, etc.). Monitor WARN logs
  at Prompt 07.
- **Terms-of-service checkbox on `CartPage`.** Demowebshop requires the
  TOS box before checkout — forgetting it produces a JS alert, which is
  quiet to debug. Explicit assertion/action needed.
- **Maven versions (April 2026).** Cited versions are best-estimate as of
  the assistant's knowledge cutoff. Verify live on Maven Central at
  Prompt 02 and log any bumps in `CLAUDE.md`.
