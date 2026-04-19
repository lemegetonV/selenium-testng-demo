# selenium-testng-demo

End-to-end Selenium automation for the demowebshop purchase flow, built with Java 21, TestNG, and Maven.

## AI-assisted development

This project was built using Claude Code (Anthropic) as the primary coding
agent, with the Playwright CLI used separately for scripted locator
exploration against the live site. The build proceeded as a fixed sequence
of prompts — planning, scaffold, locator exploration, page objects, tests,
stabilization, documentation — with the framework blueprint locked into
`AI-docs/PLAN.md` up front and each prompt's narrative recorded as a
dedicated file under `AI-docs/`. AI tool use was required per the
assessment brief; `AI-docs/` contains the actual prompts used, the
per-phase commentary, and corrections from the user, so a reviewer can
audit the process as well as the output.

## What it does

One data-driven test walks the full eleven-step purchase flow on
`https://demowebshop.tricentis.com`: launch the browser, log in, search for a
product, add it to the cart from the search result tile, review the cart,
proceed to checkout, fill billing and shipping addresses, choose a shipping
method and a payment method, enter card details, confirm the order, verify
the confirmation page, and log out. Hard assertions fire at four checkpoints —
login success, product added to cart, cart contents match the selected item,
and order confirmation displayed — with supporting assertions at intermediate
stages so a failure lands on the first wrong state rather than cascading.
The test is data-driven through a TestNG `@DataProvider` that feeds search
terms from JSON, so adding a new product scenario is a JSON edit, not a code
change.

## Quick start

```
# Prerequisites: JDK 21+, Maven 3.9+, Chrome browser installed
git clone <repo-url>
cd selenium-testng-demo
mvn clean test
```

After the run, the report lands at `reports/extent-report.html`.

## Viewing the committed report

A reference passing run is committed to `reports/extent-report.html`. Open it
directly in any browser to see evidence of the last green run without needing
to execute the suite yourself. The report includes the full step-by-step
interaction narrative — clicks, form fills, dropdown selections, the order
number captured — not just the verdict. Screenshots are base64-embedded in the
HTML, so the file is self-contained and shareable.

## Project structure

```
selenium-testng-demo/
├── pom.xml                     Maven build + dependency versions
├── testng.xml                  Suite file; registers the Extent listener
├── CLAUDE.md                   Working memory for the AI-assisted build
├── README.md                   This file
├── AI-docs/                    Prompt-by-prompt record of the build
│   ├── PLAN.md                   Locked design blueprint
│   ├── locators.md               Per-page Selenium By locators
│   └── *-*.md                    One markdown file per prompt phase
├── reports/
│   ├── extent-report.html        Committed green-run evidence
│   └── screenshots/              Failure screenshots (wiped @BeforeSuite)
├── exploration/
│   ├── explore.js                Playwright locator-discovery script
│   └── screenshots/              23 numbered screenshots, one per flow step
├── logs/                       Rolling file logs (gitignored)
└── src/
    ├── main/java/com/demowebshop/
    │   ├── config/               ConfigReader — properties + -D overrides
    │   ├── core/                 BasePage, ElementActions, FrameworkException
    │   ├── driver/               DriverFactory — ThreadLocal WebDriver
    │   ├── models/               POJOs for users, products, billing, cards
    │   ├── pages/                Page objects for the eleven-step flow
    │   └── utils/                Wait, screenshot, test data, Datafaker
    ├── main/resources/
    │   └── log4j2.xml            Console + rolling file + Extent appender
    └── test/
        ├── java/com/demowebshop/
        │   ├── base/               BaseTest — driver lifecycle + MDC
        │   ├── dataproviders/      TestNG @DataProvider for search terms
        │   ├── listeners/          ExtentReportListener, ExtentAppender
        │   └── tests/              EndToEndPurchaseTest
        └── resources/
            ├── config.properties   Environment + wait + seed config
            └── testdata/           users.json, products.json, paymentCards.json
```

## Design decisions

### Dynamic By locators, not PageFactory

Page objects resolve locators at interaction time via `driver.findElement(By...)`
inside `ElementActions`, not through `@FindBy` proxies. This makes
`StaleElementReferenceException` handling a single well-defined retry rather
than a leaky abstraction, lets parameterized locators live as plain
`String.format(template, value)` helpers, and matches the direction Selenium 4
itself has moved (PageFactory has been deprecated from the official docs).

### Single ElementActions wrapper as the only DOM entry point

Every click, type, select, check, text read, and wait in the codebase routes
through `ElementActions`. Page objects never call `driver.findElement` directly.
That centralization pays off in three places: waits are consistent (every
interaction does a `forVisible` or `forClickable` first), logging is uniform
(every action produces an INFO-level line tagged with a human-readable
description), and the click fallback chain lives in exactly one method.
Grep the codebase for `driver.findElement` — it appears only inside
`ElementActions`.

### Three-tier click fallback

`ElementActions.click` tries native Selenium click first. If it hits
`ElementClickInterceptedException` or `ElementNotInteractableException`, it
scrolls the element into view and retries. If that still fails, it falls back
to a JavaScript-executor click. Every tier logs its attempt, and the JS-tier
path logs at WARN so frequent fallbacks surface in the logs as a structural
locator smell rather than silent flakiness masked by retry. The committed
Prompt 06 run triggered zero JS fallbacks, which is the signal that the
locators harvested during exploration are structurally sound — native clicks
are succeeding across the whole flow.

### Hard assertions only, at four checkpoints

The test uses `Assert.assertTrue`/`assertEquals` and stops at the first failure.
`SoftAssert` is intentionally not introduced. In a sequential E2E flow a
soft-asserted failure at step 3 would let steps 4-11 run against wrong state
and produce a misleading failure report — far better to fail fast at the first
wrong state and know exactly what broke. The four locked checkpoints are login
success, product added to cart, cart contents match the selected item, and
order confirmation displayed; supporting assertions at page-loaded gates give
a readable failure message at every intermediate stage without weakening the
fail-fast contract.

### Test data separation

Static credentials and the search-term catalog live in JSON under
`src/test/resources/testdata/` and load through Jackson into Lombok-built
POJOs (`@Data @Builder @Jacksonized`). Dynamic data — billing addresses,
cardholder names, expiry dates, CVVs — is generated per test by
`TestDataFactory` using Datafaker seeded from `config.properties`, so a flaky
run is reproducible by re-using the same seed. Card numbers are **not**
randomized: demowebshop validates the Luhn checksum, so `paymentCards.json`
ships two hardcoded Luhn-valid test card numbers (a Visa and a Mastercard)
paired with card-type strings that match the payment dropdown exactly. A
random sixteen-digit string fails checksum validation on submit.

### Scripted locator discovery via Playwright

Before any page object was written, `exploration/explore.js` walked the live
demowebshop DOM end-to-end in headless Playwright. For each interaction it
evaluated multiple selector strategies against the live elements, captured
every dropdown's option strings (billing/shipping countries, US states,
shipping methods, payment methods, card types, expiry months and years), and
took twenty-three numbered screenshots as visual evidence. The output is
`AI-docs/locators.md` — a per-page By-expression map with parameterized
templates and dropdown value catalogs that the page objects import verbatim.
This is an unusual approach worth calling out: most candidates guess locators
or let an LLM hallucinate them; this repo harvested and verified them against
the live DOM, which is why the first stabilization run (Prompt 06) needed
only a single wait fix for an AJAX race — no locator churn.

### Logs, MDC, and Extent reports share a single test-name key

One source of truth for test identity across every artifact: the `testName`
MDC key is set in `BaseTest.@BeforeMethod` from
`ITestResult.getMethod().getMethodName()`, consumed by the log4j2 pattern
(`%X{testName}`) in both the console and rolling file appenders, and used as
the Extent node name in `ExtentReportListener.onTestStart`. A log line, a
screenshot filename, and an Extent node all carry the same test name — easy
to cross-reference when triaging a failure.

### Forwarded SLF4J logs into the Extent report

A custom Log4j2 plugin appender (`ExtentAppender`) mirrors INFO/WARN/ERROR
events from the `com.demowebshop` logger into the currently active Extent
test node, so the committed report reads as a second-by-second narrative
of the run — `Click: cart link`, `Type 'Lamar' into: billing first name`,
`Order placed successfully. Order number: 2276316` — rather than a bare
pass/fail verdict. Coupling is zero: the test writes logs for its own
purposes, the appender listens in through a `ThreadLocal<ExtentTest>` holder.
DEBUG is intentionally filtered out of the report (still captured in the
rolling file log) so the narrative stays focused on user-visible actions;
post-mortem forensics that need the click-fallback-tier detail go to
`logs/test.log`.

## How to run variations

System-property overrides beat values from `config.properties`:

```
mvn clean test -Dheadless=true -DbaseUrl=https://demowebshop.tricentis.com
```

Overridable keys in `src/test/resources/config.properties`:

- `baseUrl` — hit a different environment
- `browser` — local vs CI browser swap (shipping suite runs Chrome; key exists for future use)
- `headless` — CI runs headless, dev runs headful
- `environment` — tag reports per environment
- `dataFakerSeed` — reproduce a flaky run with the same seeded data
- `explicitWaitSeconds` — default 15s
- `pageLoadTimeoutSeconds` — default 30s

## Important note about real orders

> This test places a real order on the shared demowebshop test account every
> run. The account state is dirty by design — it is shared across every
> candidate who takes this assessment — and the test is written to never
> depend on account state (address book entries, order history). But
> reviewers re-running the suite should know that each run leaves a
> persistent order behind on the shared account.

## Project metadata

- Java 21
- Maven 3.9+
- Selenium 4.28.0
- TestNG 7.11.0
- ExtentReports 5.1.2
- Log4j2 2.24.3 + SLF4J 2.0.16
- Jackson 2.18.2
- Datafaker 2.4.2
- Lombok 1.18.38
