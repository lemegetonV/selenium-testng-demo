# Prompt 03 — Plan Amendments + Locator Exploration

**Session:** `7560d8fb-e96e-4a68-a056-70d2a5835278`
**Time:** 2026-04-19 14:50

## Summary

Two-part prompt. Part 1: apply eight plan amendments (A–H) to PLAN.md, CLAUDE.md, and test data. Part 2: write and run a headless Playwright exploration script that walks the full purchase flow, harvests stable locators, captures dropdown option strings, takes 23 numbered screenshots, and produces AI-docs/locators.md.

## Prompt text

Re-read CLAUDE.md and AI-docs/PLAN.md before you start. Those files are 
the source of truth for design decisions and code style. Follow them.

This prompt has two parts, both executed in this single turn:

Part 1 — Apply plan amendments to PLAN.md, CLAUDE.md, and config/data files.
Part 2 — Write and run a Playwright exploration script that walks the full 
         purchase flow headlessly, harvests stable locators, captures 
         dropdown option strings, takes step screenshots, and produces 
         AI-docs/locators.md.

The prompt sequence has been renumbered: Prompt 02 absorbed the reporting/
listener scope originally planned for Prompt 03, so this prompt (locator 
exploration) is now Prompt 03. Going forward: page objects = Prompt 04, 
tests = Prompt 05, stabilize = Prompt 06, readme + final AI-docs = Prompt 
07. Update CLAUDE.md Progress Log and PLAN.md §9 to reflect the renumbering.

---

## Part 1 — Plan amendments

Apply each amendment in place (edit PLAN.md directly, edit config/data files 
directly where applicable, log each amendment in CLAUDE.md's Progress Log 
entry for this prompt under a "Plan Amendments" subsection).

### A. Resolve Open Question #1 in PLAN.md §10

Checkout is confirmed as a single-page accordion with six numbered sections 
(Billing, Shipping, Shipping Method, Payment Method, Payment Information, 
Confirm Order). CheckoutPage stays as a single page object. Mark resolved.

### B. CheckoutPage — six discrete action methods

Update §3 (class inventory) and §4 (page object breakdown):

- fillBillingAddress(BillingAddress) → advances to step 2
- fillShippingAddress(BillingAddress) → advances to step 3
- selectShippingMethod(String methodName) → advances to step 4
- selectPaymentMethod(String methodName) → advances to step 5
- fillPaymentInfo(CreditCard) → advances to step 6
- confirmOrder() → returns OrderConfirmationPage

Each method: selects "New Address" in the address dropdown where applicable 
(billing + shipping), fills the form, clicks that section's Continue button, 
waits for the next section to become active before returning.

### C. BillingAddress POJO gets a `state` field

Add `state` (String). Populate in TestDataFactory.generateBillingAddress() 
using faker.address().state() — returns full state names which match the 
dropdown. Country must be hardcoded to "United States" (state dropdown is 
country-dependent; faker-generated country would break the flow).

### D. CreditCard: hardcoded Luhn-valid numbers, dynamic rest

Reason: the demo site runs Luhn checksum validation on the card number. 
Random digits fail. Solution: known-good test card numbers in JSON, 
dynamic cardholder/expiry/CVV from Datafaker.

Create new file: `src/test/resources/testdata/paymentCards.json`

    {
      "testCards": [
        {
          "cardType": "Visa",
          "cardNumber": "4111111111111111"
        },
        {
          "cardType": "Master card",
          "cardNumber": "5555555555554444"
        }
      ]
    }

Note: `cardType` values must match the demo site dropdown options EXACTLY. 
The exploration script in Part 2 will confirm the exact strings. If they 
differ from "Visa" / "Master card", update this JSON file to match.

Create new POJO: `TestCard` in com.demowebshop.models
  - fields: cardType (String), cardNumber (String)
  - @Data @Builder @Jacksonized (it's deserialized from JSON)

Update `TestDataFactory.generateCreditCard()`:
  1. Load testCards from paymentCards.json once via static block (cached)
  2. Pick the first entry (deterministic) — don't randomize card selection
  3. Populate CreditCard POJO:
     - cardType ← testCard.cardType
     - cardNumber ← testCard.cardNumber
     - cardholder ← faker.name().fullName()
     - expiryMonth ← "12" (hardcoded, safe value — any month works)
     - expiryYear ← String.valueOf(LocalDate.now().getYear() + 4) 
       (dynamic, stays valid as years pass — add comment explaining why)
     - cvv ← faker.numerify("###")

Add a javadoc on generateCreditCard() explaining the Luhn-validity 
constraint and why card numbers are hardcoded.

### E. Shipping address filled independently with same object as billing

Document in CheckoutPage class inventory entry: the shipping address 
dropdown does NOT default to the just-entered billing address. The 
deterministic approach is: always select "New Address" in the shipping 
dropdown and refill using the same BillingAddress object passed to 
fillBillingAddress.

Test method sketch (reference only):
    BillingAddress address = TestDataFactory.generateBillingAddress();
    checkoutPage.fillBillingAddress(address);
    checkoutPage.fillShippingAddress(address);

Open item to resolve during Part 2: confirm whether any "Ship to same 
address" shortcut exists in the shipping section DOM. If yes, capture 
its selector; if no, stick with refill approach.

### F. Account state is dirty

Add to CLAUDE.md open items / constraints: the test account has 
accumulated address book entries and order history from prior candidates. 
Tests MUST:
- Never assert on address book contents or ordering
- Never assert on order history count or "most recent order is ours"
- Verify order success via the confirmation page banner and order number 
  displayed there, not via order history navigation

### G. HomePage stays minimal

HomePage needs only: open() (navigate to baseUrl) and a loaded-check 
(title or known landmark presence). Most navigation is via 
HeaderComponent. Note this in §3.

### H. Search term update — one term, one product

The E2E will search for "laptop" and expect to find "14.1-inch Laptop". 
Update `src/test/resources/testdata/products.json`:

    {
      "searchTerms": [
        {
          "term": "laptop",
          "expectedProductName": "14.1-inch Laptop"
        }
      ]
    }

Rename the SearchTerm POJO field from `expectedKeyword` to 
`expectedProductName`. Update assertions in later prompts to use this 
exact product name for cart verification.

One search term is sufficient — the DataProvider pattern is demonstrated 
by ProductDataProvider existing and feeding from JSON; running the full 
E2E twice adds runtime without demonstrating new capability. Note this 
reasoning in PLAN.md §5.

---

## Part 2 — Scripted locator exploration

Write a Playwright (Node.js) script that walks the full 11-step purchase 
flow headlessly, at each interaction point evaluates multiple selector 
strategies, picks the most stable one, captures dropdown option strings, 
takes a screenshot after each step, and produces AI-docs/locators.md.

### Setup

1. Create directory: `exploration/` at project root
2. Inside exploration/:
   - package.json (Node project, commonjs or esm, your choice)
   - explore.js — the main script
   - screenshots/ — populated at runtime, committed to repo as evidence
3. Install Playwright and chromium:
    cd exploration && npm init -y && 
    npm install playwright && 
    npx playwright install chromium

If chromium install fails (network/firewall), stop and report — do not 
fabricate locators. Ask me to resolve before proceeding.

Add `exploration/node_modules/` to .gitignore. Do NOT gitignore 
`exploration/screenshots/` — those are committed as evidence.

### Script design (explore.js)

The script should:

**1. Run headless, but log verbosely.** Every action prints what it's 
doing. Every locator candidate evaluation logs the candidate and whether 
it uniquely matched (count === 1).

**2. Walk the full flow in order.** Use these credentials and test data:
- Login: qa.user123@mailinator.com / Engineer@09876
- Search term: laptop
- Expected product: 14.1-inch Laptop
- Billing/shipping: use any plausible US address values. Values don't 
  matter — selectors do.
- Shipping method: pick whichever radio is first (usually "Ground")
- Payment method: "Credit Card"
- Card: 4111111111111111, Visa, any future expiry, any 3-digit CVV

**3. At each interaction point, evaluate selector strategies in this 
preference order:**
  1. Stable `id` attribute → By.cssSelector("#someId")
  2. Stable `name` attribute → By.cssSelector("[name='foo']")
  3. Unique class combination → By.cssSelector(".class1.class2")
  4. Attribute-based → By.cssSelector("[data-testid='x']") if present
  5. Text-based XPath as last resort → 
     By.xpath("//button[normalize-space()='Submit']")

For each candidate, call `page.locator(selector).count()` to confirm it 
uniquely matches (count === 1 for single elements). Only accept locators 
that uniquely match. Log rejections.

**4. Capture dropdown option strings.** For every `<select>` element 
interacted with, extract and log the full list of option texts and 
values. These go into locators.md under a "Dropdown Values" subsection 
per page. Critical dropdowns to capture:
- Billing: Country options, State/Province options (after selecting US), 
  Address book options (the "Select a billing address..." dropdown — 
  confirm "New Address" is an available option string)
- Shipping: same address book dropdown, confirm "New Address" is present
- Shipping Method: list radio button labels (may be radios not selects)
- Payment Method: list radio button labels
- Payment Info: Credit card type options, Expiry month options, 
  Expiry year options

**5. Take a screenshot after each flow step.** Save to 
exploration/screenshots/ with names like `01-homepage.png`, 
`02-login-page.png`, `03-logged-in.png`, ..., `16-logged-out.png`. 
Numbered so folder sort-order matches flow order.

**6. Specific things to confirm / capture:**

  a. On search results for "laptop", verify "14.1-inch Laptop" is in the 
     results. Determine whether "Add to cart" is available directly on 
     the tile or requires navigating to a PDP. Capture selectors for 
     both paths if ambiguous; document which path the script actually 
     used.

  b. On add-to-cart, capture the success notification bar selector if 
     possible. If it fades too quickly, note this in locators.md and 
     suggest a waitForVisible strategy with short timeout.

  c. On the cart page, capture: the product line item locator 
     (parameterized by product name), the Terms of Service checkbox 
     locator, the Checkout button locator.

  d. On checkout billing section: capture the "New Address" dropdown 
     selector and confirm "New Address" is a selectable option string.

  e. On checkout shipping section: same as billing, plus **explicitly 
     search the DOM for any "Ship to same address" / "Same as billing" 
     shortcut**. Report found/not-found.

  f. On order confirmation page: capture the success banner text + 
     selector, the order number text + selector, and any "Continue" / 
     "Details" link selector.

  g. On logout: capture the Log out link selector in the header.

**7. If any step fails** (locator not found, action doesn't advance the 
page, assertion fails), the script should log the failure with the DOM 
snippet of the surrounding area, take a failure screenshot, and move on 
where possible (or stop if it's structurally blocking). Do not silently 
paper over failures.

**8. On completion, write AI-docs/locators.md.** See format below.

### locators.md format

**Header section:**
- Title, one-line purpose
- Date, Playwright version used, URL explored
- One-paragraph summary: flow walked successfully (yes/no), any gaps, 
  any surprises

**Per-page sections**, in flow order:
- HomePage
- HeaderComponent
- LoginPage
- SearchResultsPage
- ProductDetailsPage — only if the flow required navigating to PDP. If 
  Add to cart was available on the tile, note that PDP is unused and 
  recommend removing it from PLAN.md §3.
- CartPage
- CheckoutPage (with subsections: Billing, Shipping, Shipping Method, 
  Payment Method, Payment Info, Confirm Order)
- OrderConfirmationPage

**For each page, two tables:**

Table 1 — Locators:
| Locator name | Selenium By expression | Notes |

- Locator names: intent-revealing (emailInput, termsOfServiceCheckbox, 
  billingFirstNameInput, billingContinueButton, addToCartButtonForProduct)
- By expressions: prefer By.cssSelector for id/name/class; use By.xpath 
  only for text-based matching
- Parameterized locators: document with sample usage, e.g.:
    ADD_TO_CART_BY_PRODUCT = 
      "//h2/a[normalize-space()='%s']/ancestor::div[contains(@class,'product-item')]//input[@value='Add to cart']"
- Notes: flag brittle selectors, flag where waits are critical, flag 
  any ambiguity

Table 2 — Dropdown / radio values (only where applicable):
| Dropdown name | Option strings (exact text) |

### After locators.md is generated

- If ProductDetailsPage is unused, remove from PLAN.md §3 and note the 
  decision in the Progress Log.
- Update PLAN.md §10 open items: resolve #1 (checkout structure — 
  confirmed), #2 (add-to-cart source — resolved by exploration), #3 
  (HeaderComponent scope — confirmed worth a class if used across 
  header actions).
- Update CLAUDE.md Progress Log:
  - Amendments applied (list all 8: A through H)
  - Exploration script outcome (flow completed yes/no, any issues)
  - Screenshot folder referenced
  - Any brittle selectors flagged for attention in Prompt 04
  - Exact dropdown option strings captured (briefly — full list goes 
    in locators.md)
  - Ship-to-same-address shortcut: found or not found
  - Add-to-cart path: tile or PDP
  - Any new open items
  - Next prompt: "Prompt 04 — implement page objects using locators.md"

### Commit

After generating everything, confirm the repo state is clean. This 
prompt produces one commit:

  docs: apply plan amendments and add locator map from scripted exploration

Files touched expected:
- PLAN.md, CLAUDE.md (amendments + progress log)
- src/test/resources/testdata/products.json (updated)
- src/test/resources/testdata/paymentCards.json (new)
- src/main/java/com/demowebshop/models/TestCard.java (new)
- src/main/java/com/demowebshop/models/BillingAddress.java (state field)
- src/main/java/com/demowebshop/models/SearchTerm.java (field renamed)
- src/main/java/com/demowebshop/utils/TestDataFactory.java 
  (generateCreditCard logic, generateBillingAddress state handling)
- exploration/explore.js (new)
- exploration/package.json (new)
- exploration/screenshots/*.png (new, committed)
- AI-docs/locators.md (new)
- .gitignore (exploration/node_modules/)

## Constraints

- If Playwright install fails, stop and report. Don't fake it.
- If the exploration script fails partway through and can't complete, 
  commit what you have, document what's missing in locators.md, and 
  stop — do not fabricate locators for unwalked steps.
- Don't write any Selenium page object code. That's Prompt 04.
- The exploration script is production evidence, not throwaway — write 
  it readably, with comments, following the same Code Style Standards 
  applied to Java code (where applicable to JS).

Go.

## Outcome

See the *2026-04-19 — Prompt 03: Plan Amendments + Locator Exploration* entry in `CLAUDE.md`. Artifacts: `AI-docs/locators.md`, `exploration/explore.js`, `exploration/screenshots/01-*.png` … `23-*.png`, plan amendments A–H applied.
