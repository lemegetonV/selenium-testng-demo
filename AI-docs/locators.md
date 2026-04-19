# locators.md — Demowebshop Locator Map

**Purpose:** Stable Selenium locators for all pages in the E2E purchase flow,
harvested by scripted Playwright exploration.

| Field | Value |
|---|---|
| Date | 2026-04-19 |
| Playwright version | 1.59.1 |
| URL explored | https://demowebshop.tricentis.com |
| Flow result | ✓ Complete |

## Summary

Full purchase flow walked successfully. Selectors were evaluated using Playwright's `locator().count()`
to confirm uniqueness. Preference order: `id` → `name` → class combination →
`data-testid` → text-based XPath.

**Exploration notes:**
- headerComponent.accountLink: `a[href*="customer/info"]` matches 2 elements (main + mobile nav). Use `.header-links a[href='/customer/info']` or `.first()` in Selenium. Not needed for the current E2E flow — the test does not assert on account info.
- Search for "laptop" returns "14.1-inch Laptop" on the tile grid.
- SearchResultsPage parameterized product locator: Parameterized: css=`.product-title a` filtered by text, or XPath: //h2[contains(@class,'product-title')]/a[normalize-space()='%s']
- Add-to-cart path: TILE (no PDP navigation required). ProductDetailsPage is unused.
- Add-to-cart notification text: "The product has been added to your shopping cart"
- Ship-to-same-address shortcut NOT found. Approach: always select "New Address" in shipping dropdown and refill using same BillingAddress object.
- Order confirmation banner text: "Your order has been successfully processed!"
- orderConfirmationPage.orderNumberValue: candidates `.order-number strong` / `li.order-number` did not match. Visual inspection (screenshot 22) shows "Order number: 2276297" is plain text inside the `.section.order-completed` content div — use XPath `//div[contains(@class,'order-completed')]//p[contains(normalize-space(.),'Order number')]` or extract from the section container text with `String.split("Order number:")`.

---

## Parameterized Locator Templates

These XPath templates are parameterized by product name and should be formatted
with `String.format(template, productName)` in Java:

```
// Product tile on search results page (targets Add to Cart for a specific product):
ADD_TO_CART_BY_PRODUCT =
  "//h2[contains(@class,'product-title')]/a[normalize-space()='%s']/ancestor::div[contains(@class,'product-item')]//input[@value='Add to cart']"

// Product name link in cart:
CART_PRODUCT_BY_NAME =
  "//td[contains(@class,'product')]//a[normalize-space()='%s']"
```

---

## HomePage

### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `loginLink` | `By.cssSelector("a[href="/login"]")` | Header nav link to /login |
| `homeLandmark` | `By.cssSelector(".header-logo")` | Landmark used for page-loaded check |


---

## HeaderComponent

### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `logoutLink` | `By.cssSelector(".ico-logout")` |  |
| `searchBox` | `By.cssSelector("#small-searchterms")` |  |
| `searchButton` | `By.cssSelector("input.search-box-button")` |  |
| `cartLink` | `By.cssSelector("#topcartlink a")` |  |
| `cartQuantityBadge` | `By.cssSelector("#topcartlink .cart-qty")` | Shows item count after add-to-cart |


---

## LoginPage

### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `emailInput` | `By.cssSelector("#Email")` |  |
| `passwordInput` | `By.cssSelector("#Password")` |  |
| `loginButton` | `By.cssSelector("input[value="Log in"]")` |  |
| `loginErrorMessage` | `By.cssSelector(".message-error")` | Present only on failed login — evaluated pre-submit |


---

## SearchResultsPage

> **Add-to-cart path: TILE.** "Add to cart" is available directly on search result tiles.
> `ProductDetailsPage` is unused — recommend removing from PLAN.md §3.

### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `productTitle` | `By.cssSelector(".product-title a")` | First title link in the product grid |
| `addToCartButton` | `By.cssSelector("input[value="Add to cart"]")` | Tile-level add-to-cart. Use text filter to target specific product. |
| `addToCartNotification` | `By.cssSelector("#bar-notification")` | Success notification bar — visible briefly after add-to-cart. Use waitForVisible with short timeout. |


---

## ProductDetailsPage

_Unused — add-to-cart available directly from search tile. Remove from PLAN.md §3._

---

## CartPage

### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `productLineItem` | `By.cssSelector(".cart-item-row")` | One row per product. Filter by product name inside .product-name or td > a. |
| `productNameLink` | `By.xpath("//td[contains(@class,"product")]//a[contains(@class,"product-name")]")` | Product name link inside cart row. Used to assert the correct product was added. |
| `termsOfServiceCheckbox` | `By.cssSelector("#termsofservice")` | Must be ticked before checkout — skipping causes a JS alert. |
| `checkoutButton` | `By.cssSelector("#checkout")` |  |


---

## CheckoutPage

The checkout is a **single-page accordion** with six numbered sections.
`CheckoutPage` stays as a single page object with six discrete action methods.

### Billing

#### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `billingAddressDropdown` | `By.cssSelector("#billing-address-select")` | "Select a billing address..." dropdown. Options include saved addresses + "New Address". |
| `billingFirstNameInput` | `By.cssSelector("#BillingNewAddress_FirstName")` |  |
| `billingLastNameInput` | `By.cssSelector("#BillingNewAddress_LastName")` |  |
| `billingEmailInput` | `By.cssSelector("#BillingNewAddress_Email")` |  |
| `billingCountryDropdown` | `By.cssSelector("#BillingNewAddress_CountryId")` |  |
| `billingStateDropdown` | `By.cssSelector("#BillingNewAddress_StateProvinceId")` | Populated after country is selected. Options are US states. |
| `billingCityInput` | `By.cssSelector("#BillingNewAddress_City")` |  |
| `billingAddress1Input` | `By.cssSelector("#BillingNewAddress_Address1")` |  |
| `billingZipInput` | `By.cssSelector("#BillingNewAddress_ZipPostalCode")` |  |
| `billingPhoneInput` | `By.cssSelector("#BillingNewAddress_PhoneNumber")` |  |
| `billingContinueButton` | `By.cssSelector("#billing-buttons-container input[value="Continue"]")` |  |


#### Dropdown Values
| Dropdown name | Option strings (exact text) |
|---|---|
| Billing address book | `Aslam Attar, 2945 N Tucson Blvd, Tucson, Illinois 85716, United States`, `John Doe, 123 Test Street, Los Angeles, California 90210, United States`, `Aslam Attar, asd, ads, AA (Armed Forces Americas) ad, United States`, `a a, dd, dd dd, India`, `dd dd, dd, dd dd, India`, `User Attar, Street 1, Delhi 110001, India`, `abcd User, Street 1, Delhi 110001, India`, `Prem Sharma, Street 1, Delhi 110001, India`, `Test XYZ, Street 1, Mumbai 400001, India`, `Prem Sharma, #122, sector-61, Chandigarh, Delhi 110001, India`, `Test XYZ, #122, sector-61, Chandigarh, Mumbai 400001, India`, `Test Automation, 123 Test Street, New York, AA (Armed Forces Americas) 10001, United States`, `Test Automation, 123 Test Street, New York, New York 10001, United States`, `QA Tester, 456 Automation Ave, Austin, Texas 73301, United States`, `QA User, 123 Test Street, Bangalore 560001, India`, `QA Tester, 123 Test Street, Los Angeles 90001, United States`, `Jane Smith, 999 Broadway, New York City 10001, United States`, `QA Tester, 123 Test Street, Los Angeles, AA (Armed Forces Americas) 90001, United States`, `Aslam Attar, 12 Homee Street, Seattle, Arizona 56004, United States`, `Jane Tester, 123 Test Street, Los Angeles, California 90001, United States`, `New Address` |
| Country (first 10) | `Select country`, `United States`, `Canada`, `Afghanistan`, `Albania`, `Algeria`, `American Samoa`, `Andorra`, `Angola`, `Anguilla` |
| State/Province (first 10 of 62) | `AA (Armed Forces Americas)`, `AE (Armed Forces Europe)`, `Alabama`, `Alaska`, `American Samoa`, `AP (Armed Forces Pacific)`, `Arizona`, `Arkansas`, `California`, `Colorado` |

---

### Shipping

> Ship-to-same-address shortcut NOT found. Approach: always select "New Address" in shipping dropdown and refill using same BillingAddress object.

#### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `shippingAddressDropdown` | `By.cssSelector("#shipping-address-select")` | "Select a shipping address..." dropdown. Select "New Address" to enable form fields. |
| `shippingFirstNameInput` | `By.cssSelector("#ShippingNewAddress_FirstName")` |  |
| `shippingLastNameInput` | `By.cssSelector("#ShippingNewAddress_LastName")` |  |
| `shippingEmailInput` | `By.cssSelector("#ShippingNewAddress_Email")` |  |
| `shippingCountryDropdown` | `By.cssSelector("#ShippingNewAddress_CountryId")` |  |
| `shippingStateDropdown` | `By.cssSelector("#ShippingNewAddress_StateProvinceId")` |  |
| `shippingCityInput` | `By.cssSelector("#ShippingNewAddress_City")` |  |
| `shippingAddress1Input` | `By.cssSelector("#ShippingNewAddress_Address1")` |  |
| `shippingZipInput` | `By.cssSelector("#ShippingNewAddress_ZipPostalCode")` |  |
| `shippingPhoneInput` | `By.cssSelector("#ShippingNewAddress_PhoneNumber")` |  |
| `shippingContinueButton` | `By.cssSelector("#shipping-buttons-container input[value="Continue"]")` |  |


#### Dropdown Values
| Dropdown name | Option strings (exact text) |
|---|---|
| Shipping address book | `Aslam Attar, 2945 N Tucson Blvd, Tucson, Illinois 85716, United States`, `John Doe, 123 Test Street, Los Angeles, California 90210, United States`, `Aslam Attar, asd, ads, AA (Armed Forces Americas) ad, United States`, `a a, dd, dd dd, India`, `dd dd, dd, dd dd, India`, `User Attar, Street 1, Delhi 110001, India`, `abcd User, Street 1, Delhi 110001, India`, `Prem Sharma, Street 1, Delhi 110001, India`, `Test XYZ, Street 1, Mumbai 400001, India`, `Prem Sharma, #122, sector-61, Chandigarh, Delhi 110001, India`, `Test XYZ, #122, sector-61, Chandigarh, Mumbai 400001, India`, `Test Automation, 123 Test Street, New York, AA (Armed Forces Americas) 10001, United States`, `Test Automation, 123 Test Street, New York, New York 10001, United States`, `QA Tester, 456 Automation Ave, Austin, Texas 73301, United States`, `QA User, 123 Test Street, Bangalore 560001, India`, `QA Tester, 123 Test Street, Los Angeles 90001, United States`, `Jane Smith, 999 Broadway, New York City 10001, United States`, `QA Tester, 123 Test Street, Los Angeles, AA (Armed Forces Americas) 90001, United States`, `Aslam Attar, 12 Homee Street, Seattle, Arizona 56004, United States`, `Jane Tester, 123 Test Street, Los Angeles, California 90001, United States`, `New Address` |

---

### Shipping Method

#### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `shippingMethodRadio` | `By.cssSelector("input[name="shippingoption"]")` | Group selector — 3 options. Use .first() for Ground or filter by value. Values: ["Ground (0.00)","Next Day Air (0.00)","2nd Day Air (0.00)"] |
| `shippingMethodContinueButton` | `By.cssSelector("#shipping-method-buttons-container input[value="Continue"]")` |  |


#### Radio Values
| Radio group | Option labels |
|---|---|
| Shipping methods | `Ground (0.00)`, `Next Day Air (0.00)`, `2nd Day Air (0.00)` |

---

### Payment Method

#### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `paymentMethodRadio` | `By.cssSelector("input[name="paymentmethod"]")` | Group selector. Filter by adjacent label text to select "Credit Card". Values: ["Cash On Delivery (COD) (7.00)","Check / Money Order (5.00)","Credit Card","Purchase Order"] |
| `paymentMethodContinueButton` | `By.cssSelector("#payment-method-buttons-container input[value="Continue"]")` |  |


#### Radio Values
| Radio group | Option labels |
|---|---|
| Payment methods | `Cash On Delivery (COD) (7.00)`, `Check / Money Order (5.00)`, `Credit Card`, `Purchase Order` |

---

### Payment Info

#### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `cardTypeDropdown` | `By.cssSelector("#CreditCardType")` |  |
| `cardholderNameInput` | `By.cssSelector("#CardholderName")` |  |
| `cardNumberInput` | `By.cssSelector("#CardNumber")` |  |
| `expiryMonthDropdown` | `By.cssSelector("#ExpireMonth")` |  |
| `expiryYearDropdown` | `By.cssSelector("#ExpireYear")` |  |
| `cvvInput` | `By.cssSelector("#CardCode")` |  |
| `paymentInfoContinueButton` | `By.cssSelector("#payment-info-buttons-container input[value="Continue"]")` |  |


#### Dropdown Values
| Dropdown name | Option strings (exact text) |
|---|---|
| Credit card type | `Visa`, `Master card`, `Discover`, `Amex` |
| Expiry month | `01`, `02`, `03`, `04`, `05`, `06`, `07`, `08`, `09`, `10`, `11`, `12` |
| Expiry year | `2026`, `2027`, `2028`, `2029`, `2030`, `2031`, `2032`, `2033`, `2034`, `2035`, `2036`, `2037`, `2038`, `2039`, `2040` |

---

### Confirm Order

#### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `confirmOrderButton` | `By.cssSelector("#confirm-order-buttons-container input[value="Confirm"]")` |  |


---

## OrderConfirmationPage

### Locators
| Locator name | Selenium By expression | Notes |
|---|---|---|
| `successBannerTitle` | `By.cssSelector(".section.order-completed .title")` | The "Your order has been successfully processed!" heading. |
| `orderNumberContainer` | `By.cssSelector(".section.order-completed")` | The whole confirmation section. Call `getText()` and `split("Order number:")` to extract the numeric order ID. Standalone strong/li selectors did not match — the number is plain inline text. |
| `continueButton` | `By.cssSelector(".order-completed input[value='Continue']")` |  |

