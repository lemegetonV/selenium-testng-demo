# Prompt 01 — Planning

**Session:** `bcd753b9-62cb-435d-ab03-2a16848e958a`
**Time:** 2026-04-19 13:29

## Summary

Design blueprint for the full build. No code yet — produces PLAN.md, CLAUDE.md, and the AI-docs/ scaffold.

## Prompt text

I'm building a Selenium E2E automation project for a QA take-home assessment, 
and I want to plan it out properly before writing any code. I'd like you to 
produce a planning document that'll serve as the blueprint for the rest of 
the build.

## The assessment

I need to automate an end-to-end purchase flow on https://demowebshop.tricentis.com 
using Java, Selenium, TestNG, and Maven. The full flow is:

1. Launch browser and go to the homepage
2. Login with qa.user123@mailinator.com / Engineer@09876
3. Search for a product (e.g. "computer")
4. Add a product to the cart from search results
5. Go to the cart and verify the product is listed
6. Proceed to checkout
7. Fill billing and shipping details
8. Select a shipping method and payment method
9. Confirm the order
10. Verify the order success message
11. Logout

Assertions are required at four checkpoints: login success, product added to 
cart, cart matches the selected item, and order confirmation displayed.

## Framework design — already decided

These decisions are finalized, so please don't suggest alternatives. Just 
work them into the plan:

- **Locators**: Dynamic `By` locators, not PageFactory. Static final By 
  fields, resolved fresh at action time. Parameterized locators via helper 
  methods returning By.
- **Utilities**: DriverFactory using ThreadLocal<WebDriver>, ConfigReader 
  with typed getters and System.getProperty override, WaitUtils wrapping 
  WebDriverWait, ScreenshotUtils, TestDataReader (JSON via Jackson), 
  TestDataFactory using Datafaker and seedable for reproducibility.
- **Wrapper layer**: An ElementActions class that all page objects route 
  through. Page objects never call driver.findElement() directly. Methods 
  include click, type, selectByVisibleText, selectByValue, getText, 
  isDisplayed, isPresent, waitForText, scrollIntoView, hover.
- **Click fallback**: Three-tier chain. Native click first. On 
  ElementClickInterceptedException or ElementNotInteractableException, scroll 
  into view and retry native. Final fallback is JavascriptExecutor click. 
  Log DEBUG when native or scroll-retry succeeds, WARN when JS fallback 
  succeeds (so flakiness surfaces). StaleElementReferenceException triggers 
  a single retry of the whole chain.
- **BasePage**: abstract class that instantiates and exposes `actions` and 
  `wait` to all page objects so nothing has to be re-wired per page.
- **FrameworkException**: custom runtime exception for clearer stack traces.
- **Logging**: SLF4J with Log4j2. INFO for user actions, DEBUG for internals, 
  WARN for fallback-success cases, ERROR for failures. MDC tagging by test 
  method name. Password fields masked in type() when the description 
  contains "password".
- **Test data**: config.properties for environment config. Static data 
  (users, products) in JSON files under src/test/resources/testdata/, 
  deserialized to POJOs with Lombok @Data @Builder. Dynamic data (billing, 
  shipping, card info) generated per test via TestDataFactory.
- **Data-driven**: testng.xml parameters for browser and baseUrl. A 
  @DataProvider reads two search terms from products.json.
- **Reporting**: ExtentReports, committed to the repo so reviewers can see 
  evidence without running the project. Fixed output path at 
  `reports/extent-report.html`, screenshots base64-embedded. The screenshots 
  folder is wiped at @BeforeSuite so each committed run is clean. TestNG's 
  default test-output/ is gitignored since it's redundant.
- **Git**: conventional commits, one commit per prompt-phase, reports 
  committed, target/ and test-output/ gitignored.

## What I need you to do

First, create two files and set up the project for ongoing documentation:

1. **Create an `AI-docs/` folder** in the project root. This is where I'll be 
   dropping the prompts I use throughout this build, one file per prompt.

2. **Create `PLAN.md`** in the project root containing the full blueprint 
   (details below).

3. **Create `CLAUDE.md`** in the project root. This is your working memory 
   file for the whole build. It should:
   - Link to PLAN.md at the top as the source of truth for design decisions
   - Have a "Progress Log" section that you update after every prompt I 
     run with you, capturing: what was implemented, what decisions were 
     made (including any corrections from me), any nuances discovered 
     (e.g. a selector that didn't behave as expected, a library version 
     bump, a locator strategy tweak)
   - Have a "Open Items" section for anything deferred or pending
   - Be the file you re-read at the start of every subsequent prompt so 
     you stay consistent with prior decisions
   Please add an explicit instruction in CLAUDE.md itself that says: 
   "Update this file at the end of every implementation prompt. Record 
   what changed, why, and any corrections from the user. Re-read this 
   file at the start of every new prompt."

Then populate PLAN.md with the following sections:

1. **Project folder structure** — the full tree. Show src/main/java, 
   src/test/java, src/test/resources, reports/, AI-docs/, everything. Name 
   every package and key file, including where each utility, page object, 
   POJO, test data file, and config file lives.

2. **Maven dependencies** — exact groupId:artifactId:version for Selenium, 
   TestNG, WebDriverManager, Log4j2 + SLF4J, Jackson, Lombok, Datafaker, 
   ExtentReports, and Apache Commons IO if useful. Please use current 
   stable versions as of April 2026. If any version pin matters (e.g. a 
   known conflict), call it out.

3. **Class inventory** — list every class to be created, grouped by layer:
   - Core utilities (DriverFactory, ConfigReader, WaitUtils, ElementActions, 
     ScreenshotUtils, TestDataReader, TestDataFactory, FrameworkException, 
     BasePage)
   - POJOs (User, SearchTerm, BillingAddress, CreditCard, and any others 
     you think belong)
   - Page objects — you decide the list based on the 11-step flow above
   - Test classes and listeners
   Give each one a one-line purpose statement.

4. **Page object breakdown** — walk through the 11-step flow and identify 
   distinct pages or sections to model as page objects. Don't 
   over-fragment; group where it makes sense (e.g. if checkout is a 
   single-page accordion, one PO is enough; if it's multi-step, split it). 
   Note that you won't know this for sure until the locator exploration 
   step, so make your best guess and flag it as an assumption.

5. **Test data file shapes** — the exact JSON structure for users.json and 
   products.json with sample values.

6. **Configuration surface** — what goes in config.properties, with example 
   values. Flag which keys are overridable via -D system properties.

7. **Logging configuration** — log4j2.xml outline: appenders (console + 
   file), pattern string including MDC test name, default log levels.

8. **ExtentReports setup** — listener class design, report path, how 
   screenshots attach to failing tests, how MDC test name flows into 
   report nodes.

9. **Prompt sequence ahead** — list the remaining prompts I'll be running 
   with you, with a one-line goal each. From memory, the sequence is: 
   scaffold the framework, add reporting and listeners, run Playwright 
   CLI codegen and translate output to a locators map, build page 
   objects, write tests, stabilize after first run, finalize README and 
   AI-docs. This list becomes my AI-docs narrative.

10. **Open questions and risks** — be honest about unknowns. For example, 
    the checkout page structure may need the POs split differently once 
    we see the live DOM. Call out anything you're uncertain about now so 
    we can revisit it at the right phase.

## A few constraints

- Don't generate code yet. This is planning only.
- Don't re-open the design decisions above. They're locked.
- Keep PLAN.md scannable. Use headers, tables where they help, and code 
  blocks only for folder trees, JSON samples, and config snippets.
- The goal is that a reviewer (or future-me coming back after a break) can 
  read PLAN.md and understand the whole project shape in five minutes.

Go ahead and create the three files (AI-docs/ folder, PLAN.md, CLAUDE.md) 
and populate them as described. Also add a "Code Style Standards" section to CLAUDE.md with the following 
content verbatim (I'll paste it below), and add a note at the top of 
CLAUDE.md that says: "All generated code must follow the Code Style 
Standards section of this document. Re-check against those standards 
before completing any code-generation prompt." ## Code Style Standards (applies to all generated code)

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

## Outcome

See the *2026-04-19 — Prompt 01: Planning* entry in `CLAUDE.md` for what was produced. Artifacts committed at this phase: `AI-docs/PLAN.md`, `CLAUDE.md`, `.gitignore`.
