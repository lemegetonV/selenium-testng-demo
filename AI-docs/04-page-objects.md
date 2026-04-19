# Prompt 04 — Page Object Implementation

**Session:** `1842e1bb-f381-4241-bfbb-061945129a4a`
**Time:** 2026-04-19 15:15

## Summary

Real implementation of all seven page objects using AI-docs/locators.md as the source of truth for every By expression. No test logic yet — tests come in Prompt 05.

## Prompt text

Re-read CLAUDE.md, AI-docs/PLAN.md, and AI-docs/locators.md before you 
start. Those three files are the source of truth. locators.md is 
especially important — every By expression in this prompt comes from 
there, not from memory or guesswork.

This prompt implements all page objects fully. Real code, not stubs. No 
test logic yet — tests are Prompt 05, stabilization is Prompt 06.

## Page objects to implement

Per PLAN.md §3 (updated after Prompt 03), the page object list is:

- HomePage
- HeaderComponent
- LoginPage
- SearchResultsPage
- CartPage
- CheckoutPage
- OrderConfirmationPage

ProductDetailsPage was removed from §3 after Prompt 03 confirmed the 
tile-based Add to Cart path. Do not recreate it.

## Design rules — non-negotiable

**1. All DOM interactions route through ElementActions.** Page objects 
NEVER call driver.findElement(), .click(), .sendKeys() etc. directly. If 
you find yourself wanting to — stop, add the method to ElementActions 
instead. The only direct driver usage permitted in a page object is for 
things ElementActions doesn't do (e.g. getCurrentUrl, navigate, 
getTitle). Radio-button and dropdown workarounds live in ElementActions 
or in a small page-object-local helper method, not inline.

**2. Locators are private static final By fields.** One block near the 
top of each class. Copy the By expressions verbatim from locators.md. 
Parameterized locators are private static methods returning By that take 
the dynamic value as a parameter.

**3. Fluent API where navigation happens.** Methods that cause a page 
transition return the next page object. Methods that stay on the same 
page return void or the same page type (for chaining). Examples:
- LoginPage.login(User) → HomePage
- HeaderComponent.clickLogin() → LoginPage
- HeaderComponent.searchFor(String) → SearchResultsPage
- HeaderComponent.openCart() → CartPage
- SearchResultsPage.addProductToCart(String) → void (stays on page; 
  notification appears)
- CartPage.proceedToCheckout() → CheckoutPage
- CheckoutPage section methods → void (same page, next accordion section)
- CheckoutPage.confirmOrder() → OrderConfirmationPage
- OrderConfirmationPage.continueShopping() → HomePage (if used)

**4. Each page object has a loaded-check.** A public boolean isLoaded() 
method that verifies a landmark element is visible. Used by tests to 
assert landing on the right page. BasePage may provide a helper if 
pattern is identical across pages, but favor a small per-page 
implementation that checks a page-specific element.

**5. Code style discipline from CLAUDE.md applies.** Methods under 20 
lines; javadoc on public action methods; no commented-out code; no 
System.out.println.

## Per-page specifications

### HomePage
- Locators: header logo or equivalent landmark from locators.md
- Methods:
  - `open()` — navigates to baseUrl via driver.get(); reads baseUrl 
    from ConfigReader
  - `isLoaded()` — confirms header logo visible
- Minimal class per PLAN.md amendment G.

### HeaderComponent
The header is present on every page. Modeled as a class extending 
BasePage. Instantiated when needed.

Methods:
- `clickLoginLink()` → LoginPage
- `searchFor(String term)` → SearchResultsPage — types term into search 
  box, clicks search button (not Enter — locators.md captured the button)
- `openCart()` → CartPage — clicks the "Shopping cart" link
- `clickLogout()` → HomePage — clicks Log out link; waits for login link 
  to reappear as confirmation
- `isLoggedIn()` boolean — checks whether the "Log out" link is present 
  (or equivalent state signal per locators.md)
- `getCartItemCount()` int — parses the count from the cart link text 
  "Shopping cart(N)"; returns 0 if cart link text doesn't contain a 
  numeric count
- Note on accountLink: per CLAUDE.md open items, there are 2 DOM matches. 
  Don't add an accountLink method unless later prompts need it.

### LoginPage
- Locators: email input, password input, login button, error summary 
  (per locators.md)
- Methods:
  - `login(User user)` → HomePage — types email (plain), types password 
    (masked — description contains "password"), clicks Login button, 
    waits for header state change (log-out link visible) before returning
  - `isLoaded()` — confirms email input visible
  - `getErrorMessage()` String — returns text of login error summary if 
    present, empty string otherwise. Used by negative tests later if 
    added.

### SearchResultsPage
- Locators: parameterized locator for product tile by name, parameterized 
  "Add to cart" button on tile, notification bar (`#bar-notification`)
- Methods:
  - `isLoaded()` — confirms the search results grid landmark is visible
  - `containsProduct(String productName)` boolean — returns true if a 
    tile with the given product name exists on the page
  - `addProductToCart(String productName)` — clicks Add to cart on the 
    tile matching productName. After the click, waits briefly for 
    `#bar-notification` to appear as success signal. Returns void; tests 
    chain with HeaderComponent.openCart() to move on.
- Do NOT wait for the notification to disappear — it may have already 
  faded by the time the wait registers. Just confirm it appeared.

### CartPage
- Locators: product name column (parameterized or list), Terms of 
  Service checkbox (`#termsofservice`), Checkout button (`#checkout`), 
  cart item row locator
- Methods:
  - `isLoaded()` — confirms the cart table or equivalent landmark
  - `getCartItemNames()` List<String> — returns the text of each product 
    name in the cart table; used by tests for cart-matches-selected-item 
    assertion. Iterate via driver.findElements routed through a 
    helper-added ElementActions method `findAllTexts(By)` if it doesn't 
    already exist; if it doesn't, add it now with the same 
    log → wait → act → wrap pattern as other ElementActions methods.
  - `acceptTermsOfService()` — ticks `#termsofservice`. Uses 
    ElementActions.click (the checkbox click path is safe for native).
  - `proceedToCheckout()` → CheckoutPage — clicks `#checkout` button, 
    waits for CheckoutPage landmark before returning
  - Composite convenience method `checkout()` → CheckoutPage that calls 
    acceptTermsOfService() + proceedToCheckout() in sequence. Tests will 
    prefer this.

### CheckoutPage
Six discrete action methods per PLAN.md amendment B. Each method:
- Fills or selects the required fields for its accordion section
- Clicks the Continue button for that section
- Waits for the next section to become active before returning

Methods:
- `fillBillingAddress(BillingAddress address)` void
  - Select "New Address" on billing dropdown via 
    ElementActions.selectByVisibleText (dropdown value for New Address 
    is "" — do NOT use selectByValue per CLAUDE.md open items)
  - Fill: firstName, lastName, email, country ("United States"), state, 
    city, address1, zip, phone
  - Click billing Continue
  - Wait for shipping section active state
- `fillShippingAddress(BillingAddress address)` void
  - Same pattern. Same address object (per PLAN.md amendment E). No 
    "ship to same" shortcut exists per Prompt 03 finding.
- `selectShippingMethod(String methodName)` void
  - The shipping options are radios sharing `name="shippingoption"`. 
    Find the radio whose adjacent label text contains methodName 
    (e.g. "Ground (0.00)") and click the radio.
  - Implement this as a small private helper method in CheckoutPage: 
    `private void selectRadioByLabel(By groupLocator, String labelText)` 
    — uses driver.findElements on the group, iterates to find matching 
    label, calls ElementActions.click on the matching radio by a 
    parameterized By. Alternatively build a text-based XPath that 
    locates the radio directly:
    `//input[@name='shippingoption'][following-sibling::label[contains(normalize-space(.), '%s')]]`
    Prefer the XPath approach if it's unique — it's one line and keeps 
    the logic in ElementActions routing.
  - Click shipping-method Continue
  - Wait for payment-method section active
- `selectPaymentMethod(String methodName)` void
  - Same radio-by-label pattern; `name="paymentmethod"`; label text 
    examples: "Credit Card", "Check / Money Order (5.00)".
  - Click payment-method Continue
  - Wait for payment-info section active
- `fillPaymentInfo(CreditCard card)` void
  - Select credit card type by visible text ("Visa", "Master card", etc.)
  - Fill cardholder, cardNumber
  - Select expiry month by visible text
  - Select expiry year by visible text
  - Fill CVV (description must contain "cvv" for masking)
  - Click payment-info Continue
  - Wait for confirm-order section active
- `confirmOrder()` → OrderConfirmationPage
  - Click the Confirm button
  - Wait for `.section.order-completed` to become visible before 
    returning OrderConfirmationPage
  - Per Prompt 03 discovery: the confirm button's navigation races 
    with URL change. Waiting on the success section selector is the 
    reliable signal.

Don't implement a composite "complete checkout" method. Tests call the 
six methods in sequence — explicit, reviewable, maps 1:1 to the 
assessment's flow steps 7–9.

### OrderConfirmationPage
- Locators: success banner (`.section.order-completed` or landmark per 
  locators.md), continue button / details link
- Methods:
  - `isLoaded()` — confirms the order-completed section visible
  - `isOrderPlaced()` boolean — returns true if the success banner text 
    matches expected pattern ("Your order has been successfully 
    processed!" or whatever locators.md captured)
  - `getOrderNumber()` String — reads the full text of the 
    order-completed section, parses after "Order number: ", returns the 
    trimmed number. Returns empty string if not found — assertion is the 
    test's job, not the page object's.
  - `continueShopping()` → HomePage — only if locators.md captured a 
    continue link; otherwise skip this method

## Helper additions to ElementActions (if needed)

You MAY need to add these to ElementActions during this prompt. Add them 
only if used; keep the class growth minimal.

- `findAllTexts(By locator)` List<String> — returns the text of every 
  matched element. For getCartItemNames().
- `selectByVisibleText` and `selectByValue` already exist (scaffolded in 
  Prompt 02) — use them as-is.
- `check(By locator, String description)` void — for checkboxes, wraps 
  click with idempotence (only clicks if not already selected). Useful 
  for TOS.
  - If this doesn't exist yet, add it with the same 
    log → wait → act → wrap pattern.

Any ElementActions additions get a javadoc and follow the same fallback 
discipline as the existing methods. Log failures, capture screenshot, 
wrap in FrameworkException.

## What NOT to do in this prompt

- No test class logic. EndToEndPurchaseTest stays as a stub until 
  Prompt 05.
- No running the suite. Don't `mvn test` — compile check is fine 
  (`mvn compile` to confirm no compile errors), but no tests run yet.
- No retry analyzers, no custom listeners beyond what Prompt 02 already 
  created.
- No additional utility classes beyond ElementActions additions listed 
  above. If you think you need another utility, stop and ask me first.
- No new POJOs. All models are defined.

## Verification before you finish

Walk your own output and confirm:
1. Every locator used matches an entry in locators.md. If you can't 
   find an entry, stop — don't invent. Update locators.md if a new 
   locator genuinely emerged during implementation (e.g. a radio-label 
   parameterization template that wasn't in locators.md as a template).
2. No page object file contains `driver.findElement(`, 
   `driver.findElements(` outside of the ElementActions helper 
   additions. Grep yourself before declaring done.
3. Every public action method has a one-line javadoc.
4. Every fluent return type matches the specification above.
5. No TODOs, no commented-out blocks, no System.out.println.
6. `mvn compile` passes.

## Update CLAUDE.md

Append a Progress Log entry for Prompt 04 with:
- Files created/modified (page objects, any ElementActions additions)
- Any locators.md updates made during implementation (e.g. a 
  parameterized template added) — with reasoning
- Any nuances discovered (e.g. a radio button that needed a different 
  approach than planned, a wait that turned out to need tuning)
- Any open items resolved
- Any new open items for Prompt 05/06
- Next prompt: "Prompt 05 — write the EndToEndPurchaseTest using the 
  page objects"

## Commit

One commit for this prompt:

  feat: implement page objects for full purchase flow

Go.

## Outcome

See the *2026-04-19 — Prompt 04: Page Object Implementation* entry in `CLAUDE.md`. Seven page objects implemented; two new `ElementActions` methods (`findAllTexts`, `check`); Lombok 1.18.38 upgrade and the `-J--add-opens` compiler args applied to fix the Java 21.0.10 annotation-processor crash.
