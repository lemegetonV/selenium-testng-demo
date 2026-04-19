/**
 * Demowebshop purchase-flow locator explorer.
 *
 * Walks the full 11-step flow headlessly, evaluates multiple selector
 * strategies at each interaction point, captures dropdown/radio option
 * strings, takes step screenshots, and writes AI-docs/locators.md.
 *
 * Run: node explore.js  (from inside exploration/)
 */

'use strict';

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------

const BASE_URL = 'https://demowebshop.tricentis.com';
const CREDENTIALS = { email: 'qa.user123@mailinator.com', password: 'Engineer@09876' };
const SEARCH_TERM = 'laptop';
const EXPECTED_PRODUCT = '14.1-inch Laptop';
const TEST_CARD = { number: '4111111111111111', type: 'Visa', expMonth: '12', expYear: '2029', cvv: '123' };

const SCREENSHOTS_DIR = path.join(__dirname, 'screenshots');
const LOCATORS_MD_PATH = path.join(__dirname, '..', 'AI-docs', 'locators.md');
const DEFAULT_TIMEOUT = 15000;

// ---------------------------------------------------------------------------
// State collected during exploration
// ---------------------------------------------------------------------------

/** Collected locators per page, each entry: { name, selector, notes } */
const collected = {
  homePage: [],
  headerComponent: [],
  loginPage: [],
  searchResultsPage: [],
  productDetailsPageUsed: false,
  productDetailsPage: [],
  cartPage: [],
  checkoutBilling: [],
  checkoutShipping: [],
  checkoutShippingMethod: [],
  checkoutPaymentMethod: [],
  checkoutPaymentInfo: [],
  checkoutConfirmOrder: [],
  orderConfirmationPage: [],
};

/** Dropdown/radio option strings captured per section */
const dropdowns = {
  billingCountry: [],
  billingState: [],
  billingAddressBook: [],
  shippingAddressBook: [],
  shippingMethodRadios: [],
  paymentMethodRadios: [],
  cardTypeOptions: [],
  expiryMonthOptions: [],
  expiryYearOptions: [],
};

/** Flow-level notes and discoveries */
const explorationNotes = [];

let stepCounter = 0;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function log(message) {
  console.log(`[EXPLORE] ${message}`);
}

async function screenshot(page, label) {
  stepCounter++;
  const paddedStep = String(stepCounter).padStart(2, '0');
  const filename = `${paddedStep}-${label}.png`;
  const filepath = path.join(SCREENSHOTS_DIR, filename);
  await page.screenshot({ path: filepath, fullPage: false });
  log(`Screenshot saved: ${filename}`);
}

/**
 * Evaluates candidate selectors in preference order and returns the first
 * one that uniquely matches (count === 1). Logs each candidate and outcome.
 * Returns null if nothing matched uniquely.
 */
async function evaluateLocator(page, candidates) {
  for (const candidate of candidates) {
    let count;
    try {
      count = await page.locator(candidate).count();
    } catch {
      log(`  SKIP ${candidate} — locator() threw`);
      continue;
    }
    if (count === 1) {
      log(`  ACCEPT ${candidate} (count=1)`);
      return candidate;
    }
    log(`  REJECT ${candidate} (count=${count})`);
  }
  log(`  WARN — no unique match among candidates: ${JSON.stringify(candidates)}`);
  return null;
}

/** Wait for a selector to be visible and unique, then return the locator string. */
async function waitAndEvaluate(page, candidates, timeout = DEFAULT_TIMEOUT) {
  const best = await evaluateLocator(page, candidates);
  if (best) {
    await page.locator(best).first().waitFor({ state: 'visible', timeout });
  }
  return best;
}

async function captureSelectOptions(page, selector) {
  try {
    return await page.locator(selector).evaluate(el =>
      Array.from(el.options).map(o => ({ text: o.text.trim(), value: o.value }))
    );
  } catch (err) {
    log(`  WARN captureSelectOptions failed for ${selector}: ${err.message}`);
    return [];
  }
}

async function captureRadioLabels(page, radioGroupSelector) {
  try {
    return await page.locator(radioGroupSelector).evaluateAll(els =>
      els.map(el => {
        const label = el.closest('label') || document.querySelector(`label[for="${el.id}"]`);
        return label ? label.textContent.trim() : el.value;
      })
    );
  } catch (err) {
    log(`  WARN captureRadioLabels failed for ${radioGroupSelector}: ${err.message}`);
    return [];
  }
}

function addLocator(section, name, selector, notes = '') {
  if (selector) {
    collected[section].push({ name, selector, notes });
    log(`  LOCATOR recorded [${section}] ${name} → ${selector}`);
  } else {
    explorationNotes.push(`MISSING locator: ${section}.${name} — no unique selector found`);
    log(`  ERROR no locator for [${section}] ${name}`);
  }
}

// ---------------------------------------------------------------------------
// Flow steps
// ---------------------------------------------------------------------------

async function stepHomepage(page) {
  log('=== STEP: Homepage ===');
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded', timeout: 30000 });
  await screenshot(page, 'homepage');

  const loginLink = await evaluateLocator(page, [
    'a[href="/login"]',
    'text=Log in',
    '//a[normalize-space()="Log in"]',
  ]);
  addLocator('homePage', 'loginLink', loginLink, 'Header nav link to /login');

  const homeTitle = await evaluateLocator(page, [
    '.header-logo',
    '#logo',
    '.home-page',
  ]);
  addLocator('homePage', 'homeLandmark', homeTitle, 'Landmark used for page-loaded check');
}

async function stepNavigateToLogin(page) {
  log('=== STEP: Navigate to login ===');
  await page.click('a[href="/login"]');
  await page.waitForURL('**/login', { timeout: DEFAULT_TIMEOUT });
  await screenshot(page, 'login-page');

  const emailInput = await evaluateLocator(page, [
    '#Email',
    '[name="Email"]',
    'input[type="email"]',
  ]);
  addLocator('loginPage', 'emailInput', emailInput);

  const passwordInput = await evaluateLocator(page, [
    '#Password',
    '[name="Password"]',
    'input[type="password"]',
  ]);
  addLocator('loginPage', 'passwordInput', passwordInput);

  const loginButton = await evaluateLocator(page, [
    'input[value="Log in"]',
    '.login-button',
    '//input[@value="Log in"]',
  ]);
  addLocator('loginPage', 'loginButton', loginButton);

  const errorMessage = await evaluateLocator(page, [
    '.validation-summary-errors',
    '.message-error',
    '.field-validation-error',
  ]);
  addLocator('loginPage', 'loginErrorMessage', errorMessage, 'Present only on failed login — evaluated pre-submit');
}

async function stepLogin(page) {
  log('=== STEP: Fill login form ===');
  await page.fill('#Email', CREDENTIALS.email);
  await page.fill('#Password', CREDENTIALS.password);
  await page.click('input[value="Log in"]');
  await page.waitForURL(/^(?!.*login).*$/, { timeout: DEFAULT_TIMEOUT });
  await screenshot(page, 'logged-in');

  // Capture header locators on logged-in state
  const accountLink = await evaluateLocator(page, [
    '.ico-account',
    'a.account',
    `a[href*="customer/info"]`,
    '//a[contains(@class,"account")]',
  ]);
  addLocator('headerComponent', 'accountLink', accountLink);

  const logoutLink = await evaluateLocator(page, [
    '.ico-logout',
    'a[href="/logout"]',
    '//a[@href="/logout"]',
  ]);
  addLocator('headerComponent', 'logoutLink', logoutLink);

  const searchBox = await evaluateLocator(page, [
    '#small-searchterms',
    '[name="q"]',
    'input.search-box',
  ]);
  addLocator('headerComponent', 'searchBox', searchBox);

  const searchButton = await evaluateLocator(page, [
    'input.search-box-button',
    '[value="Search"]',
    '//input[@class="search-box-button"]',
  ]);
  addLocator('headerComponent', 'searchButton', searchButton);

  const cartLink = await evaluateLocator(page, [
    '#topcartlink a',
    '.cart-label',
    'a[href="/cart"]',
  ]);
  addLocator('headerComponent', 'cartLink', cartLink);

  const cartQty = await evaluateLocator(page, [
    '#topcartlink .cart-qty',
    '.cart-qty',
  ]);
  addLocator('headerComponent', 'cartQuantityBadge', cartQty, 'Shows item count after add-to-cart');
}

async function stepSearch(page) {
  log('=== STEP: Search for laptop ===');
  await page.fill('#small-searchterms', SEARCH_TERM);
  await page.click('input.search-box-button');
  await page.waitForURL('**/search*', { timeout: DEFAULT_TIMEOUT });
  await screenshot(page, 'search-results');

  // Verify expected product is present
  const productTileText = await page.locator('.product-title').allTextContents();
  const found = productTileText.some(t => t.includes(EXPECTED_PRODUCT));
  if (!found) {
    explorationNotes.push(`WARNING: "${EXPECTED_PRODUCT}" NOT found in search results. Titles seen: ${JSON.stringify(productTileText)}`);
    log(`  ERROR expected product "${EXPECTED_PRODUCT}" not in results`);
  } else {
    log(`  OK product "${EXPECTED_PRODUCT}" found in results`);
    explorationNotes.push(`Search for "${SEARCH_TERM}" returns "${EXPECTED_PRODUCT}" on the tile grid.`);
  }

  // Check whether Add to Cart is available directly on the tile
  const tileAddToCartCount = await page.locator(`input[value="Add to cart"]`).count();
  log(`  Tile "Add to cart" buttons: ${tileAddToCartCount}`);
  collected.productDetailsPageUsed = tileAddToCartCount === 0;

  const productTitle = await evaluateLocator(page, [
    '.product-title a',
    'h2.product-title a',
    '.product-item .product-title a',
  ]);
  addLocator('searchResultsPage', 'productTitle', productTitle, 'First title link in the product grid');

  // Parameterized locator for finding a product by name
  const paramProductNote =
    'Parameterized: css=`.product-title a` filtered by text, or XPath: ' +
    `//h2[contains(@class,'product-title')]/a[normalize-space()='%s']`;
  explorationNotes.push(`SearchResultsPage parameterized product locator: ${paramProductNote}`);

  if (tileAddToCartCount > 0) {
    // Add to cart is available directly on the tile
    const tileAddToCart = await evaluateLocator(page, [
      'input[value="Add to cart"]',
      '.product-item input[value="Add to cart"]',
    ]);
    addLocator('searchResultsPage', 'addToCartButton', tileAddToCart,
      'Tile-level add-to-cart. Use text filter to target specific product.');
    explorationNotes.push('Add-to-cart path: TILE (no PDP navigation required). ProductDetailsPage is unused.');
  } else {
    explorationNotes.push('Add-to-cart NOT available on tile — will navigate to PDP.');
    collected.productDetailsPageUsed = true;
  }
}

async function stepAddToCart(page) {
  log('=== STEP: Add to cart ===');

  if (collected.productDetailsPageUsed) {
    // Navigate to PDP via the product title link
    log('  Navigating to PDP because no tile-level add-to-cart found');
    const targetLink = page.locator('.product-title a').filter({ hasText: EXPECTED_PRODUCT });
    await targetLink.click();
    await page.waitForLoadState('domcontentloaded', { timeout: DEFAULT_TIMEOUT });
    await screenshot(page, 'product-detail-page');

    const pdpAddToCart = await evaluateLocator(page, [
      '#add-to-cart-button-\\d+',
      'input[value="Add to cart"]',
      '.add-to-cart-button',
    ]);
    addLocator('productDetailsPage', 'addToCartButton', pdpAddToCart);

    const pdpProductName = await evaluateLocator(page, [
      '.product-name h1',
      'h1.product-name',
      '.product-name',
    ]);
    addLocator('productDetailsPage', 'productNameHeading', pdpProductName);

    await page.click('input[value="Add to cart"]');
  } else {
    // Click the add-to-cart button for our specific product from the tile
    const targetAddToCart = page.locator(
      `//h2[contains(@class,'product-title')]/a[normalize-space()='${EXPECTED_PRODUCT}']/ancestor::div[contains(@class,'product-item')]//input[@value='Add to cart']`
    );
    const targetCount = await targetAddToCart.count();
    if (targetCount === 1) {
      await targetAddToCart.click();
    } else {
      log(`  WARN XPath tile approach found ${targetCount} buttons; falling back to first tile add-to-cart`);
      await page.locator('input[value="Add to cart"]').first().click();
    }
  }

  await screenshot(page, 'add-to-cart-clicked');

  // Capture notification bar — it may fade quickly
  try {
    const notificationBar = await page.locator('#bar-notification').waitFor({ state: 'visible', timeout: 5000 });
    const notifSelector = await evaluateLocator(page, [
      '#bar-notification',
      '.bar-notification',
    ]);
    addLocator('searchResultsPage', 'addToCartNotification', notifSelector,
      'Success notification bar — visible briefly after add-to-cart. Use waitForVisible with short timeout.');
    const notifText = await page.locator('#bar-notification').textContent();
    explorationNotes.push(`Add-to-cart notification text: "${notifText.trim()}"`);
  } catch {
    explorationNotes.push('Add-to-cart notification bar faded before capture — use page.locator("#bar-notification").waitFor({state:"visible", timeout:3000}) immediately after click.');
    log('  WARN notification bar not captured (may have faded)');
  }

  await screenshot(page, 'after-add-to-cart');
}

async function stepGoToCart(page) {
  log('=== STEP: Navigate to cart ===');
  await page.click('#topcartlink a');
  await page.waitForURL('**/cart', { timeout: DEFAULT_TIMEOUT });
  await screenshot(page, 'cart-page');

  // Product line item — by product name text
  const productRow = await evaluateLocator(page, [
    '.cart-item-row',
    'tr.cart-item-row',
    '.shopping-cart-page tbody tr',
  ]);
  addLocator('cartPage', 'productLineItem', productRow,
    'One row per product. Filter by product name inside .product-name or td > a.');

  const productNameInCart = await evaluateLocator(page, [
    '.cart td.product .product-name a',
    '.cart .product-name a',
    '//td[contains(@class,"product")]//a[contains(@class,"product-name")]',
  ]);
  addLocator('cartPage', 'productNameLink', productNameInCart,
    'Product name link inside cart row. Used to assert the correct product was added.');

  const termsCheckbox = await evaluateLocator(page, [
    '#termsofservice',
    '[name="termsofservice"]',
    'input[type="checkbox"][name="termsofservice"]',
  ]);
  addLocator('cartPage', 'termsOfServiceCheckbox', termsCheckbox,
    'Must be ticked before checkout — skipping causes a JS alert.');

  const checkoutButton = await evaluateLocator(page, [
    '#checkout',
    '[value="Checkout"]',
    'button#checkout',
  ]);
  addLocator('cartPage', 'checkoutButton', checkoutButton);

  // Tick TOS and proceed
  await page.check('#termsofservice');
  await screenshot(page, 'cart-tos-ticked');
  await page.click('#checkout');
  await page.waitForURL('**/onepagecheckout', { timeout: DEFAULT_TIMEOUT });
  await screenshot(page, 'checkout-page-loaded');
}

async function stepCheckoutBilling(page) {
  log('=== STEP: Checkout — Billing section ===');

  // Billing address dropdown (select billing address / "New Address")
  const billingDropdown = await evaluateLocator(page, [
    '#billing-address-select',
    '[name="billing_address_id"]',
    'select#billing-address-select',
  ]);
  addLocator('checkoutBilling', 'billingAddressDropdown', billingDropdown,
    '"Select a billing address..." dropdown. Options include saved addresses + "New Address".');

  if (billingDropdown) {
    const billingAddrOptions = await captureSelectOptions(page, billingDropdown);
    dropdowns.billingAddressBook = billingAddrOptions.map(o => o.text);
    log(`  Billing address dropdown options: ${JSON.stringify(dropdowns.billingAddressBook)}`);

    // Select "New Address" — find the option whose text starts with "New"
    const newAddressOption = billingAddrOptions.find(o => o.text.trim() === 'New Address');
    if (newAddressOption) {
      await page.selectOption(billingDropdown, { value: newAddressOption.value });
      log(`  Selected billing "New Address" option: "${newAddressOption.text}" (value="${newAddressOption.value}")`);
      // Give the page a moment to react to the dropdown change before polling for form visibility
      await page.waitForTimeout(800);
    } else {
      explorationNotes.push('WARNING: No "New Address" option found in billing dropdown. Options: ' + JSON.stringify(dropdowns.billingAddressBook));
    }
  }

  // Wait for form to appear
  await page.waitForSelector('#BillingNewAddress_FirstName', { state: 'visible', timeout: DEFAULT_TIMEOUT });

  const billingFirstName = await evaluateLocator(page, [
    '#BillingNewAddress_FirstName',
    '[name="BillingNewAddress.FirstName"]',
  ]);
  addLocator('checkoutBilling', 'billingFirstNameInput', billingFirstName);

  const billingLastName = await evaluateLocator(page, [
    '#BillingNewAddress_LastName',
    '[name="BillingNewAddress.LastName"]',
  ]);
  addLocator('checkoutBilling', 'billingLastNameInput', billingLastName);

  const billingEmail = await evaluateLocator(page, [
    '#BillingNewAddress_Email',
    '[name="BillingNewAddress.Email"]',
  ]);
  addLocator('checkoutBilling', 'billingEmailInput', billingEmail);

  const billingCountry = await evaluateLocator(page, [
    '#BillingNewAddress_CountryId',
    '[name="BillingNewAddress.CountryId"]',
    'select#BillingNewAddress_CountryId',
  ]);
  addLocator('checkoutBilling', 'billingCountryDropdown', billingCountry);

  if (billingCountry) {
    const countryOptions = await captureSelectOptions(page, billingCountry);
    dropdowns.billingCountry = countryOptions.map(o => o.text).filter(t => t.trim()).slice(0, 10);
    log(`  Billing country options (first 10): ${JSON.stringify(dropdowns.billingCountry)}`);

    // Select United States
    await page.selectOption(billingCountry, { label: 'United States' });
    log('  Selected country: United States');

    // Wait for state dropdown to populate
    await page.waitForTimeout(1000);
  }

  const billingState = await evaluateLocator(page, [
    '#BillingNewAddress_StateProvinceId',
    '[name="BillingNewAddress.StateProvinceId"]',
    'select#BillingNewAddress_StateProvinceId',
  ]);
  addLocator('checkoutBilling', 'billingStateDropdown', billingState,
    'Populated after country is selected. Options are US states.');

  if (billingState) {
    const stateOptions = await captureSelectOptions(page, billingState);
    dropdowns.billingState = stateOptions.map(o => o.text).filter(t => t.trim() && t !== 'Other');
    log(`  Billing state options count: ${dropdowns.billingState.length}`);

    // Pick California as a test value
    const caOption = stateOptions.find(o => o.text.includes('California'));
    if (caOption) {
      await page.selectOption(billingState, { value: caOption.value });
      log('  Selected state: California');
    }
  }

  const billingCity = await evaluateLocator(page, [
    '#BillingNewAddress_City',
    '[name="BillingNewAddress.City"]',
  ]);
  addLocator('checkoutBilling', 'billingCityInput', billingCity);

  const billingAddress1 = await evaluateLocator(page, [
    '#BillingNewAddress_Address1',
    '[name="BillingNewAddress.Address1"]',
  ]);
  addLocator('checkoutBilling', 'billingAddress1Input', billingAddress1);

  const billingZip = await evaluateLocator(page, [
    '#BillingNewAddress_ZipPostalCode',
    '[name="BillingNewAddress.ZipPostalCode"]',
  ]);
  addLocator('checkoutBilling', 'billingZipInput', billingZip);

  const billingPhone = await evaluateLocator(page, [
    '#BillingNewAddress_PhoneNumber',
    '[name="BillingNewAddress.PhoneNumber"]',
  ]);
  addLocator('checkoutBilling', 'billingPhoneInput', billingPhone);

  // Fill form with test data
  await page.fill('#BillingNewAddress_FirstName', 'Jane');
  await page.fill('#BillingNewAddress_LastName', 'Tester');
  await page.fill('#BillingNewAddress_Email', 'jane.tester@example.com');
  await page.fill('#BillingNewAddress_City', 'Los Angeles');
  await page.fill('#BillingNewAddress_Address1', '123 Test Street');
  await page.fill('#BillingNewAddress_ZipPostalCode', '90001');
  await page.fill('#BillingNewAddress_PhoneNumber', '2135550123');

  const billingContinueButton = await evaluateLocator(page, [
    '#billing-buttons-container input[value="Continue"]',
    '#billing-buttons-container button',
    '.billing-buttons input[type="button"]',
    '//div[@id="billing-buttons-container"]//input[@value="Continue"]',
  ]);
  addLocator('checkoutBilling', 'billingContinueButton', billingContinueButton);

  await screenshot(page, 'billing-form-filled');
  await page.click('#billing-buttons-container input[value="Continue"]');

  // Wait for shipping section to become active
  await page.waitForSelector('#shipping-method-block', { state: 'hidden', timeout: DEFAULT_TIMEOUT })
    .catch(() => log('  shipping-method-block hidden check skipped'));
  await page.waitForSelector('#shipping-address-select, #ShippingNewAddress_FirstName', { state: 'visible', timeout: DEFAULT_TIMEOUT });
  await screenshot(page, 'billing-done');
}

async function stepCheckoutShipping(page) {
  log('=== STEP: Checkout — Shipping section ===');

  // Check for "Ship to same address" shortcut
  const shipToSameCount = await page.locator(
    'input[name="ship_to_same_address"], #ship-to-same-address, [name="ShipToSameAddress"]'
  ).count();
  if (shipToSameCount > 0) {
    explorationNotes.push('Ship-to-same-address shortcut FOUND — selector: [name="ShipToSameAddress"] or similar. See checkout shipping section.');
    const shipToSame = await evaluateLocator(page, [
      'input[name="ship_to_same_address"]',
      '#ship-to-same-address',
      '[name="ShipToSameAddress"]',
    ]);
    addLocator('checkoutShipping', 'shipToSameAddressCheckbox', shipToSame,
      'Shortcut to copy billing address to shipping. If checked, shipping form fields are hidden.');
    log('  Ship-to-same-address shortcut: FOUND');
  } else {
    explorationNotes.push('Ship-to-same-address shortcut NOT found. Approach: always select "New Address" in shipping dropdown and refill using same BillingAddress object.');
    log('  Ship-to-same-address shortcut: NOT FOUND');
  }

  const shippingDropdown = await evaluateLocator(page, [
    '#shipping-address-select',
    '[name="shipping_address_id"]',
    'select#shipping-address-select',
  ]);
  addLocator('checkoutShipping', 'shippingAddressDropdown', shippingDropdown,
    '"Select a shipping address..." dropdown. Select "New Address" to enable form fields.');

  if (shippingDropdown) {
    const shippingAddrOptions = await captureSelectOptions(page, shippingDropdown);
    dropdowns.shippingAddressBook = shippingAddrOptions.map(o => o.text);
    log(`  Shipping address options: ${JSON.stringify(dropdowns.shippingAddressBook)}`);

    const newAddressOption = shippingAddrOptions.find(o => o.text.trim() === 'New Address');
    if (newAddressOption) {
      await page.selectOption(shippingDropdown, { value: newAddressOption.value });
      log(`  Selected shipping "New Address": "${newAddressOption.text}"`);
    }
  }

  // Wait for shipping form fields
  await page.waitForSelector('#ShippingNewAddress_FirstName', { state: 'visible', timeout: DEFAULT_TIMEOUT })
    .catch(() => log('  ShippingNewAddress_FirstName did not appear — may use same-as-billing'));

  const shippingFirstName = await evaluateLocator(page, [
    '#ShippingNewAddress_FirstName',
    '[name="ShippingNewAddress.FirstName"]',
  ]);
  addLocator('checkoutShipping', 'shippingFirstNameInput', shippingFirstName);

  const shippingLastName = await evaluateLocator(page, [
    '#ShippingNewAddress_LastName',
    '[name="ShippingNewAddress.LastName"]',
  ]);
  addLocator('checkoutShipping', 'shippingLastNameInput', shippingLastName);

  const shippingEmail = await evaluateLocator(page, [
    '#ShippingNewAddress_Email',
    '[name="ShippingNewAddress.Email"]',
  ]);
  addLocator('checkoutShipping', 'shippingEmailInput', shippingEmail);

  const shippingCountry = await evaluateLocator(page, [
    '#ShippingNewAddress_CountryId',
    '[name="ShippingNewAddress.CountryId"]',
  ]);
  addLocator('checkoutShipping', 'shippingCountryDropdown', shippingCountry);

  const shippingState = await evaluateLocator(page, [
    '#ShippingNewAddress_StateProvinceId',
    '[name="ShippingNewAddress.StateProvinceId"]',
  ]);
  addLocator('checkoutShipping', 'shippingStateDropdown', shippingState);

  const shippingCity = await evaluateLocator(page, [
    '#ShippingNewAddress_City',
    '[name="ShippingNewAddress.City"]',
  ]);
  addLocator('checkoutShipping', 'shippingCityInput', shippingCity);

  const shippingAddress1 = await evaluateLocator(page, [
    '#ShippingNewAddress_Address1',
    '[name="ShippingNewAddress.Address1"]',
  ]);
  addLocator('checkoutShipping', 'shippingAddress1Input', shippingAddress1);

  const shippingZip = await evaluateLocator(page, [
    '#ShippingNewAddress_ZipPostalCode',
    '[name="ShippingNewAddress.ZipPostalCode"]',
  ]);
  addLocator('checkoutShipping', 'shippingZipInput', shippingZip);

  const shippingPhone = await evaluateLocator(page, [
    '#ShippingNewAddress_PhoneNumber',
    '[name="ShippingNewAddress.PhoneNumber"]',
  ]);
  addLocator('checkoutShipping', 'shippingPhoneInput', shippingPhone);

  // Fill shipping form (same data as billing)
  const shippingFormVisible = await page.locator('#ShippingNewAddress_FirstName').isVisible().catch(() => false);
  if (shippingFormVisible) {
    await page.selectOption('#ShippingNewAddress_CountryId', { label: 'United States' });
    await page.waitForTimeout(1000);
    const caOption = (await captureSelectOptions(page, '#ShippingNewAddress_StateProvinceId'))
      .find(o => o.text.includes('California'));
    if (caOption) {
      await page.selectOption('#ShippingNewAddress_StateProvinceId', { value: caOption.value });
    }
    await page.fill('#ShippingNewAddress_FirstName', 'Jane');
    await page.fill('#ShippingNewAddress_LastName', 'Tester');
    await page.fill('#ShippingNewAddress_Email', 'jane.tester@example.com');
    await page.fill('#ShippingNewAddress_City', 'Los Angeles');
    await page.fill('#ShippingNewAddress_Address1', '123 Test Street');
    await page.fill('#ShippingNewAddress_ZipPostalCode', '90001');
    await page.fill('#ShippingNewAddress_PhoneNumber', '2135550123');
  }

  const shippingContinueButton = await evaluateLocator(page, [
    '#shipping-buttons-container input[value="Continue"]',
    '//div[@id="shipping-buttons-container"]//input[@value="Continue"]',
  ]);
  addLocator('checkoutShipping', 'shippingContinueButton', shippingContinueButton);

  await screenshot(page, 'shipping-form-filled');
  await page.click('#shipping-buttons-container input[value="Continue"]');
  // Wait for shipping method radios, which appear when the shipping-method accordion opens
  await page.waitForSelector('input[name="shippingoption"]', { state: 'visible', timeout: 25000 });
  await screenshot(page, 'shipping-done');
}

async function stepCheckoutShippingMethod(page) {
  log('=== STEP: Checkout — Shipping Method section ===');

  await page.waitForSelector('input[name="shippingoption"]', { timeout: DEFAULT_TIMEOUT });

  // Capture all shipping method radio labels
  const shippingRadios = await page.locator('input[name="shippingoption"]').all();
  for (const radio of shippingRadios) {
    const radioId = await radio.getAttribute('id');
    let labelText = '';
    if (radioId) {
      const labelEl = page.locator(`label[for="${radioId}"]`);
      if (await labelEl.count() > 0) {
        labelText = (await labelEl.textContent()).trim();
      }
    }
    if (!labelText) {
      labelText = await radio.getAttribute('value') || '';
    }
    dropdowns.shippingMethodRadios.push(labelText);
  }
  log(`  Shipping method options: ${JSON.stringify(dropdowns.shippingMethodRadios)}`);

  // Radio groups match multiple elements — skip count-1 check and document the group selector.
  addLocator('checkoutShippingMethod', 'shippingMethodRadio', 'input[name="shippingoption"]',
    `Group selector — ${dropdowns.shippingMethodRadios.length} options. Use .first() for Ground or filter by value. Values: ${JSON.stringify(dropdowns.shippingMethodRadios)}`);

  // Select first radio (Ground / whichever is first)
  await page.locator('input[name="shippingoption"]').first().check();

  const shippingMethodContinue = await evaluateLocator(page, [
    '#shipping-method-buttons-container input[value="Continue"]',
    '//div[@id="shipping-method-buttons-container"]//input[@value="Continue"]',
  ]);
  addLocator('checkoutShippingMethod', 'shippingMethodContinueButton', shippingMethodContinue);

  await screenshot(page, 'shipping-method-selected');
  await page.click('#shipping-method-buttons-container input[value="Continue"]');
  // Wait for payment method radios to appear (block containers are always in DOM)
  await page.waitForSelector('input[name="paymentmethod"]', { state: 'visible', timeout: 25000 });
  await screenshot(page, 'shipping-method-done');
}

async function stepCheckoutPaymentMethod(page) {
  log('=== STEP: Checkout — Payment Method section ===');

  await page.waitForSelector('input[name="paymentmethod"]', { timeout: DEFAULT_TIMEOUT });

  // Capture all payment method radio labels
  const paymentRadios = await page.locator('input[name="paymentmethod"]').all();
  for (const radio of paymentRadios) {
    const radioId = await radio.getAttribute('id');
    let labelText = '';
    if (radioId) {
      // Some payment options render two labels (one for the logo, one for the text).
      // Use .last() to get the text label which always has the full display name.
      const labelEls = page.locator(`label[for="${radioId}"]`);
      const labelCount = await labelEls.count();
      if (labelCount > 0) {
        labelText = (await labelEls.last().textContent()).trim();
      }
    }
    if (!labelText) {
      labelText = await radio.getAttribute('value') || '';
    }
    dropdowns.paymentMethodRadios.push(labelText);
  }
  log(`  Payment method options: ${JSON.stringify(dropdowns.paymentMethodRadios)}`);

  // Radio group — document directly without count-1 check
  addLocator('checkoutPaymentMethod', 'paymentMethodRadio', 'input[name="paymentmethod"]',
    `Group selector. Filter by adjacent label text to select "Credit Card". Values: ${JSON.stringify(dropdowns.paymentMethodRadios)}`);

  // Select Credit Card — find the radio whose label includes "Credit Card"
  const paymentRadioEls = await page.locator('input[name="paymentmethod"]').all();
  let selectedCredit = false;
  for (const radio of paymentRadioEls) {
    const radioId = await radio.getAttribute('id');
    if (radioId) {
      const labelEls = page.locator(`label[for="${radioId}"]`);
      const labelCount = await labelEls.count();
      const labelText = labelCount > 0
        ? (await labelEls.last().textContent().catch(() => ''))
        : '';
      if (labelText.includes('Credit Card')) {
        await radio.check();
        selectedCredit = true;
        log(`  Selected payment method: "${labelText.trim()}"`);
        break;
      }
    }
  }
  if (!selectedCredit) {
    await page.locator('input[name="paymentmethod"]').first().check();
    log('  WARN: Could not find Credit Card payment method — selected first available');
    explorationNotes.push('WARNING: Credit Card payment method not found by label — check paymentMethodRadios dropdown values.');
  }

  const paymentMethodContinue = await evaluateLocator(page, [
    '#payment-method-buttons-container input[value="Continue"]',
    '//div[@id="payment-method-buttons-container"]//input[@value="Continue"]',
  ]);
  addLocator('checkoutPaymentMethod', 'paymentMethodContinueButton', paymentMethodContinue);

  await screenshot(page, 'payment-method-selected');
  await page.click('#payment-method-buttons-container input[value="Continue"]');
  // Wait for card type dropdown to appear (block containers are always in DOM)
  await page.waitForSelector('#CreditCardType', { state: 'visible', timeout: 25000 });
  await screenshot(page, 'payment-method-done');
}

async function stepCheckoutPaymentInfo(page) {
  log('=== STEP: Checkout — Payment Info section ===');

  // Already waited for #CreditCardType in the previous step's exit condition
  await page.waitForSelector('#CreditCardType', { state: 'visible', timeout: DEFAULT_TIMEOUT });

  const cardTypeDropdown = await evaluateLocator(page, [
    '#CreditCardType',
    '[name="CreditCardType"]',
    'select#CreditCardType',
  ]);
  addLocator('checkoutPaymentInfo', 'cardTypeDropdown', cardTypeDropdown);

  if (cardTypeDropdown) {
    const cardTypeOptions = await captureSelectOptions(page, cardTypeDropdown);
    dropdowns.cardTypeOptions = cardTypeOptions.map(o => o.text).filter(t => t.trim());
    log(`  Card type options: ${JSON.stringify(dropdowns.cardTypeOptions)}`);
    await page.selectOption(cardTypeDropdown, { label: TEST_CARD.type });
  }

  const cardholderName = await evaluateLocator(page, [
    '#CardholderName',
    '[name="CardholderName"]',
  ]);
  addLocator('checkoutPaymentInfo', 'cardholderNameInput', cardholderName);

  const cardNumber = await evaluateLocator(page, [
    '#CardNumber',
    '[name="CardNumber"]',
  ]);
  addLocator('checkoutPaymentInfo', 'cardNumberInput', cardNumber);

  const expiryMonth = await evaluateLocator(page, [
    '#ExpireMonth',
    '[name="ExpireMonth"]',
    'select#ExpireMonth',
  ]);
  addLocator('checkoutPaymentInfo', 'expiryMonthDropdown', expiryMonth);

  if (expiryMonth) {
    const monthOptions = await captureSelectOptions(page, expiryMonth);
    dropdowns.expiryMonthOptions = monthOptions.map(o => o.text).filter(t => t.trim());
    log(`  Expiry month options: ${JSON.stringify(dropdowns.expiryMonthOptions)}`);
    await page.selectOption(expiryMonth, { value: TEST_CARD.expMonth });
  }

  const expiryYear = await evaluateLocator(page, [
    '#ExpireYear',
    '[name="ExpireYear"]',
    'select#ExpireYear',
  ]);
  addLocator('checkoutPaymentInfo', 'expiryYearDropdown', expiryYear);

  if (expiryYear) {
    const yearOptions = await captureSelectOptions(page, expiryYear);
    dropdowns.expiryYearOptions = yearOptions.map(o => o.text).filter(t => t.trim());
    log(`  Expiry year options: ${JSON.stringify(dropdowns.expiryYearOptions)}`);
    // Find the closest available year to our target
    const targetYear = TEST_CARD.expYear;
    const yearOption = yearOptions.find(o => o.text.includes(targetYear) || o.value === targetYear);
    if (yearOption) {
      await page.selectOption(expiryYear, { value: yearOption.value });
    } else {
      // Pick last option (furthest future year available)
      const lastOption = yearOptions[yearOptions.length - 1];
      if (lastOption) await page.selectOption(expiryYear, { value: lastOption.value });
    }
  }

  const cvv = await evaluateLocator(page, [
    '#CardCode',
    '[name="CardCode"]',
  ]);
  addLocator('checkoutPaymentInfo', 'cvvInput', cvv);

  // Fill payment info
  await page.fill('#CardholderName', 'Jane Tester');
  await page.fill('#CardNumber', TEST_CARD.number);
  await page.fill('#CardCode', TEST_CARD.cvv);

  const paymentInfoContinue = await evaluateLocator(page, [
    '#payment-info-buttons-container input[value="Continue"]',
    '//div[@id="payment-info-buttons-container"]//input[@value="Continue"]',
  ]);
  addLocator('checkoutPaymentInfo', 'paymentInfoContinueButton', paymentInfoContinue);

  await screenshot(page, 'payment-info-filled');
  await page.click('#payment-info-buttons-container input[value="Continue"]');
  // Wait for Confirm button to appear
  await page.waitForSelector('#confirm-order-buttons-container input[value="Confirm"]', { state: 'visible', timeout: 25000 });
  await screenshot(page, 'payment-info-done');
}

async function stepCheckoutConfirmOrder(page) {
  log('=== STEP: Checkout — Confirm Order section ===');

  const confirmButton = await evaluateLocator(page, [
    '#confirm-order-buttons-container input[value="Confirm"]',
    '#confirm-order-buttons-container button',
    '//div[@id="confirm-order-buttons-container"]//input[@value="Confirm"]',
  ]);
  addLocator('checkoutConfirmOrder', 'confirmOrderButton', confirmButton);

  await screenshot(page, 'confirm-order-page');
  // Combine click + navigation into a single promise to avoid the race where
  // navigation completes before waitForURL registers the listener.
  await Promise.all([
    page.waitForSelector('.section.order-completed, .order-completed', { timeout: 30000 }),
    page.click('#confirm-order-buttons-container input[value="Confirm"]'),
  ]);
  await screenshot(page, 'order-placed');
}

async function stepOrderConfirmation(page) {
  log('=== STEP: Order confirmation page ===');

  const successBanner = await evaluateLocator(page, [
    '.section.order-completed .title',
    '.order-completed .title',
    '//div[contains(@class,"order-completed")]//strong',
  ]);
  addLocator('orderConfirmationPage', 'successBannerTitle', successBanner,
    'The "Your order has been successfully processed!" heading.');

  const successText = await page.locator('.section.order-completed .title, .order-completed .title')
    .first().textContent().catch(() => '');
  explorationNotes.push(`Order confirmation banner text: "${successText.trim()}"`);
  log(`  Banner text: "${successText.trim()}"`);

  const orderNumber = await evaluateLocator(page, [
    '.order-number strong',
    '.order-completed .order-number',
    '//li[@class="order-number"]//strong',
  ]);
  addLocator('orderConfirmationPage', 'orderNumberValue', orderNumber,
    'Strong element holding the numeric order number. Use textContent() to extract.');

  const orderNumberText = await page.locator('.order-number strong, .order-completed .order-number')
    .first().textContent().catch(() => '');
  explorationNotes.push(`Order number element text sample: "${orderNumberText.trim()}"`);

  const continueButton = await evaluateLocator(page, [
    '.order-completed-page a.btn[href="/"]',
    '.order-completed input[value="Continue"]',
    '//a[normalize-space()="Continue"]',
  ]);
  addLocator('orderConfirmationPage', 'continueButton', continueButton);

  await screenshot(page, 'order-confirmation');
}

async function stepLogout(page) {
  log('=== STEP: Logout ===');
  await page.click('a[href="/logout"]');
  await page.waitForURL(BASE_URL + '/', { timeout: DEFAULT_TIMEOUT });
  await screenshot(page, 'logged-out');
}

// ---------------------------------------------------------------------------
// Locators.md writer
// ---------------------------------------------------------------------------

function formatLocatorTable(locators) {
  if (!locators || locators.length === 0) return '_No locators captured._\n';
  const header = '| Locator name | Selenium By expression | Notes |\n|---|---|---|\n';
  const rows = locators.map(({ name, selector, notes }) => {
    const byExpr = selector.startsWith('//')
      ? `By.xpath("${selector}")`
      : `By.cssSelector("${selector}")`;
    return `| \`${name}\` | \`${byExpr}\` | ${notes} |`;
  });
  return header + rows.join('\n') + '\n';
}

function formatDropdownTable(values, label) {
  if (!values || values.length === 0) return '';
  return `| ${label} | ${values.map(v => `\`${v}\``).join(', ')} |\n`;
}

function writeLocatorsMd(playwrightVersion, flowSuccess) {
  const date = new Date().toISOString().slice(0, 10);
  const summary = flowSuccess
    ? 'Full purchase flow walked successfully.'
    : 'Flow walked with some gaps — see exploration notes section for details.';

  const md = `# locators.md — Demowebshop Locator Map

**Purpose:** Stable Selenium locators for all pages in the E2E purchase flow,
harvested by scripted Playwright exploration.

| Field | Value |
|---|---|
| Date | ${date} |
| Playwright version | ${playwrightVersion} |
| URL explored | ${BASE_URL} |
| Flow result | ${flowSuccess ? '✓ Complete' : '✗ Partial — see notes'} |

## Summary

${summary} Selectors were evaluated using Playwright's \`locator().count()\`
to confirm uniqueness. Preference order: \`id\` → \`name\` → class combination →
\`data-testid\` → text-based XPath.

${explorationNotes.length > 0 ? '**Exploration notes:**\n' + explorationNotes.map(n => `- ${n}`).join('\n') : ''}

---

## Parameterized Locator Templates

These XPath templates are parameterized by product name and should be formatted
with \`String.format(template, productName)\` in Java:

\`\`\`
// Product tile on search results page (targets Add to Cart for a specific product):
ADD_TO_CART_BY_PRODUCT =
  "//h2[contains(@class,'product-title')]/a[normalize-space()='%s']/ancestor::div[contains(@class,'product-item')]//input[@value='Add to cart']"

// Product name link in cart:
CART_PRODUCT_BY_NAME =
  "//td[contains(@class,'product')]//a[normalize-space()='%s']"
\`\`\`

---

## HomePage

### Locators
${formatLocatorTable(collected.homePage)}

---

## HeaderComponent

### Locators
${formatLocatorTable(collected.headerComponent)}

---

## LoginPage

### Locators
${formatLocatorTable(collected.loginPage)}

---

## SearchResultsPage

${collected.productDetailsPageUsed
    ? '> **Add-to-cart path: PDP.** The search result tiles do NOT expose "Add to cart" directly.\n> The flow navigates to the Product Details Page. `ProductDetailsPage` is required.'
    : '> **Add-to-cart path: TILE.** "Add to cart" is available directly on search result tiles.\n> `ProductDetailsPage` is unused — recommend removing from PLAN.md §3.'}

### Locators
${formatLocatorTable(collected.searchResultsPage)}

---

${collected.productDetailsPageUsed ? `## ProductDetailsPage

### Locators
${formatLocatorTable(collected.productDetailsPage)}

---

` : '## ProductDetailsPage\n\n_Unused — add-to-cart available directly from search tile. Remove from PLAN.md §3._\n\n---\n\n'}## CartPage

### Locators
${formatLocatorTable(collected.cartPage)}

---

## CheckoutPage

The checkout is a **single-page accordion** with six numbered sections.
\`CheckoutPage\` stays as a single page object with six discrete action methods.

### Billing

#### Locators
${formatLocatorTable(collected.checkoutBilling)}

#### Dropdown Values
| Dropdown name | Option strings (exact text) |
|---|---|
${formatDropdownTable(dropdowns.billingAddressBook, 'Billing address book')}${formatDropdownTable(dropdowns.billingCountry, 'Country (first 10)')}${formatDropdownTable(dropdowns.billingState.slice(0, 10), 'State/Province (first 10 of ' + dropdowns.billingState.length + ')')}
---

### Shipping

${explorationNotes.find(n => n.toLowerCase().includes('ship-to-same'))
    ? '> ' + explorationNotes.find(n => n.toLowerCase().includes('ship-to-same'))
    : ''}

#### Locators
${formatLocatorTable(collected.checkoutShipping)}

#### Dropdown Values
| Dropdown name | Option strings (exact text) |
|---|---|
${formatDropdownTable(dropdowns.shippingAddressBook, 'Shipping address book')}
---

### Shipping Method

#### Locators
${formatLocatorTable(collected.checkoutShippingMethod)}

#### Radio Values
| Radio group | Option labels |
|---|---|
${formatDropdownTable(dropdowns.shippingMethodRadios, 'Shipping methods')}
---

### Payment Method

#### Locators
${formatLocatorTable(collected.checkoutPaymentMethod)}

#### Radio Values
| Radio group | Option labels |
|---|---|
${formatDropdownTable(dropdowns.paymentMethodRadios, 'Payment methods')}
---

### Payment Info

#### Locators
${formatLocatorTable(collected.checkoutPaymentInfo)}

#### Dropdown Values
| Dropdown name | Option strings (exact text) |
|---|---|
${formatDropdownTable(dropdowns.cardTypeOptions, 'Credit card type')}${formatDropdownTable(dropdowns.expiryMonthOptions, 'Expiry month')}${formatDropdownTable(dropdowns.expiryYearOptions, 'Expiry year')}
---

### Confirm Order

#### Locators
${formatLocatorTable(collected.checkoutConfirmOrder)}

---

## OrderConfirmationPage

### Locators
${formatLocatorTable(collected.orderConfirmationPage)}
`;

  fs.writeFileSync(LOCATORS_MD_PATH, md, 'utf8');
  log(`locators.md written to: ${LOCATORS_MD_PATH}`);
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main() {
  // Ensure screenshots directory exists
  if (!fs.existsSync(SCREENSHOTS_DIR)) {
    fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
  }

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
  });
  const page = await context.newPage();
  page.setDefaultTimeout(DEFAULT_TIMEOUT);

  let flowSuccess = true;

  // Determine Playwright version for the report
  let playwrightVersion = 'unknown';
  try {
    const pkg = JSON.parse(fs.readFileSync(path.join(__dirname, 'node_modules', 'playwright', 'package.json'), 'utf8'));
    playwrightVersion = pkg.version;
  } catch { /* best effort */ }

  log(`Starting exploration — Playwright ${playwrightVersion}, target: ${BASE_URL}`);

  try {
    await stepHomepage(page);
    await stepNavigateToLogin(page);
    await stepLogin(page);
    await stepSearch(page);
    await stepAddToCart(page);
    await stepGoToCart(page);
    await stepCheckoutBilling(page);
    await stepCheckoutShipping(page);
    await stepCheckoutShippingMethod(page);
    await stepCheckoutPaymentMethod(page);
    await stepCheckoutPaymentInfo(page);
    await stepCheckoutConfirmOrder(page);
    await stepOrderConfirmation(page);
    await stepLogout(page);
    log('Flow completed successfully.');
  } catch (err) {
    flowSuccess = false;
    explorationNotes.push(`FATAL ERROR at step ${stepCounter}: ${err.message}`);
    log(`ERROR: ${err.stack}`);
    try {
      await screenshot(page, `failure-step${stepCounter}`);
    } catch { /* ignore screenshot failures */ }
  }

  await browser.close();

  writeLocatorsMd(playwrightVersion, flowSuccess);
  log(`Done. Screenshots: ${SCREENSHOTS_DIR}`);
  log(`locators.md: ${LOCATORS_MD_PATH}`);

  if (!flowSuccess) process.exit(1);
}

main().catch(err => {
  console.error('[EXPLORE] Fatal startup error:', err);
  process.exit(1);
});
