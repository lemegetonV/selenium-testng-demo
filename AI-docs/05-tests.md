# Prompt 05 — End-to-End Test Implementation

**Session:** `cf301031-eabb-4219-a16d-8b1079dcd7b2`
**Time:** 2026-04-19 15:36

## Summary

Implementation of EndToEndPurchaseTest — the single data-driven test that walks all eleven steps and fires hard assertions at the four locked checkpoints. No framework or page object changes unless a genuine gap is found (confirm with user first).

## Prompt text

Re-read CLAUDE.md, AI-docs/PLAN.md, and AI-docs/locators.md before you 
start.

This prompt writes the actual end-to-end test that exercises all seven 
page objects. No framework changes, no page object changes (unless a 
genuine gap is discovered — in which case stop and confirm with me 
first, don't silently patch). Implementation only.

## The test

File: `src/test/java/com/demowebshop/tests/EndToEndPurchaseTest.java`

Replace the stub @Test method with a real data-driven test that walks 
the full 11-step purchase flow. Class already extends BaseTest — keep 
that.

### Test shape

    @Test(dataProvider = "searchTerms", 
          dataProviderClass = ProductDataProvider.class,
          description = "End-to-end purchase flow: login, search, add to cart, checkout, confirm, logout")
    public void endToEndPurchaseFlow(SearchTerm searchTerm) {
        // flow implementation
    }

### Flow to implement — 11 steps from the assessment

Map each step to explicit page object calls. No hidden navigation, no 
multi-step helper methods inside the test. The test reads top-to-bottom 
like a narrative of what a user does. Assertions at the four checkpoints 
specified in the assessment.

Step-by-step:

1. **Launch and navigate.** BaseTest has already initialized the driver 
   and navigated to baseUrl. Instantiate HomePage, confirm isLoaded().

2. **Login.** 
   - Load User from users.json via TestDataReader (User.class)
   - HomePage → HeaderComponent.clickLoginLink() → LoginPage
   - LoginPage.login(user) → HomePage
   - **ASSERTION 1 (login success)**: assert HeaderComponent.isLoggedIn() 
     is true

3. **Search.**
   - HeaderComponent.searchFor(searchTerm.getTerm()) → SearchResultsPage
   - Confirm SearchResultsPage.isLoaded()
   - Assert SearchResultsPage.containsProduct(searchTerm.getExpectedProductName())

4. **Add to cart.**
   - SearchResultsPage.addProductToCart(searchTerm.getExpectedProductName())
   - **ASSERTION 2 (product added to cart)**: assert 
     HeaderComponent.getCartItemCount() > 0
     (The notification bar is ephemeral; cart count is the reliable 
     post-add signal per Prompt 04 progress log.)

5. **Navigate to cart and verify.**
   - HeaderComponent.openCart() → CartPage
   - Confirm CartPage.isLoaded()
   - **ASSERTION 3 (cart matches selected item)**: assert 
     CartPage.getCartItemNames() contains exactly 
     searchTerm.getExpectedProductName()
     Use Assert.assertTrue(names.contains(expected)) with a clear 
     assertion message like "Cart should contain '14.1-inch Laptop' 
     but had: " + names

6. **Proceed to checkout.**
   - CartPage.checkout() → CheckoutPage (this ticks TOS and clicks 
     Checkout in one call)
   - Confirm CheckoutPage.isLoaded()

7. **Fill billing and shipping.**
   - Generate ONE BillingAddress via TestDataFactory.generateBillingAddress()
   - Save to a local variable `address`
   - checkoutPage.fillBillingAddress(address)
   - checkoutPage.fillShippingAddress(address) — same address object 
     per PLAN.md amendment E

8. **Shipping and payment method.**
   - checkoutPage.selectShippingMethod("Ground") — short label works; 
     the XPath uses `contains(normalize-space(.), ...)` so it'll match 
     "Ground (0.00)"
   - checkoutPage.selectPaymentMethod("Credit Card")
   - Note: these exact strings come from locators.md dropdown values. 
     If they need to be parameterized later (env-specific pricing), 
     we'll promote to test data — for now, inline as string literals 
     with a brief comment noting the source.

9. **Payment info and confirm.**
   - Generate CreditCard via TestDataFactory.generateCreditCard() 
     (uses Visa test card per PLAN.md amendment D)
   - checkoutPage.fillPaymentInfo(card)
   - OrderConfirmationPage confirmation = checkoutPage.confirmOrder()

10. **Verify order success.**
    - Assert confirmation.isLoaded()
    - **ASSERTION 4 (order confirmation displayed)**: assert 
      confirmation.isOrderPlaced() is true
    - Capture the order number via confirmation.getOrderNumber()
    - Log it at INFO level for evidence in the report (don't assert 
      specific format — just assert it's non-empty). Numbers will vary 
      across runs.

11. **Logout.**
    - New HeaderComponent(driver).clickLogout() → HomePage
    - Assert new HeaderComponent(driver).isLoggedIn() is false
    - This is a sanity check, not one of the assessment's required 
      assertions — keep it as a regular assertTrue with a message.

### Assertions — use hard assertions (TestNG Assert)

No SoftAssert. Per PLAN.md amendment (prompt 02). Use these forms:
- Assert.assertTrue(condition, "message")
- Assert.assertEquals(actual, expected, "message")
- Assert.assertFalse(...)

Every assertion gets a meaningful message that would help a reviewer 
understand what failed without reading the test. "Cart should contain 
'14.1-inch Laptop'" is good; "assertion failed" is not.

### Logging

- Log at INFO level at the start of each major block (login, search, 
  cart, checkout, confirm, logout) so the test log reads like a flow 
  narrative
- The log.info calls inside ElementActions already cover the 
  per-interaction granularity — test-level logging is just 
  section headers:
  
      log.info("=== Step 2: Login ===");
      
- Log the order number captured in step 10 at INFO so it lands in the 
  Extent report

- @Slf4j on the test class (Lombok handles it) — matches page object 
  logging style

### Test data loading

- Users loaded once: TestDataReader.load("users.json", User.class).getDefaultUser() 
  in a @BeforeClass or read fresh inside the test — single-test class 
  so either is fine. Prefer fresh inside the test; it's simpler and no 
  state to manage.
- SearchTerm comes from the DataProvider; don't load separately.

### Things to explicitly AVOID in this test

- No direct driver calls (no driver.findElement, driver.get, etc.). 
  BaseTest handles navigation.
- No Thread.sleep. Waits are handled by page objects.
- No try-catch around assertions. Let them fail naturally — the 
  listener captures the screenshot.
- No composite "do everything" helper method. The test IS the flow.
- No conditional logic (if/else for flow branches). One straight path.
- No multiple test methods. ONE @Test method, data-driven. The 
  assessment specifies one flow; demonstrating one test class with one 
  method driven by DataProvider is sufficient.

### Handling the one-search-term case

Per PLAN.md amendment H, products.json has one searchTerm. The 
DataProvider will produce one row, the test runs once per data row 
(i.e., once total). Don't add a second term. The data-driven PATTERN 
is demonstrated by the DataProvider class existing and being wired in.

## Update testng.xml (if needed)

Confirm testng.xml already registers:
- The ExtentReportListener
- The tests package under a <test> block
- Suite parameters browser and baseUrl

If anything's missing, add it. Don't change the listener class or add 
new ones.

## Do NOT run the suite

Compile check only (`mvn clean compile test-compile`). First real run 
is Prompt 06 (stabilize), where we'll run, triage, and tune.

Specifically do NOT `mvn test`. That will actually execute the test 
against the live site, commit changes to the dirty test account, and 
generate report artifacts that we want to come from a deliberate 
Prompt 06 run.

## Update CLAUDE.md

Append a Progress Log entry for Prompt 05 with:
- Files modified (EndToEndPurchaseTest.java, any testng.xml fixes)
- Any discoveries during implementation (e.g. a page object method 
  that turned out to need a small signature tweak — if any)
- Whether all four checkpoint assertions are in place, and which line 
  each lives on
- Confirmation that `mvn test-compile` passes
- Any open items for Prompt 06
- Next prompt: "Prompt 06 — run the suite, triage failures, stabilize"

## Commit

One commit:

  test: implement end-to-end purchase flow with four-checkpoint assertions

Go.

## Outcome

See the *2026-04-19 — Prompt 05: End-to-End Test Implementation* entry in `CLAUDE.md`. One `UsersFile` wrapper POJO introduced to match the described load API without flattening `users.json`.
