package com.demowebshop.pages;

import com.demowebshop.core.BasePage;
import com.demowebshop.models.BillingAddress;
import com.demowebshop.models.CreditCard;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Single-page checkout accordion. The six action methods map 1-to-1 to the
 * accordion sections; tests call them in order to walk the checkout flow.
 */
public class CheckoutPage extends BasePage {

    // --- Billing section ---
    private static final By BILLING_ADDRESS_DROPDOWN = By.cssSelector("#billing-address-select");
    private static final By BILLING_FIRST_NAME  = By.cssSelector("#BillingNewAddress_FirstName");
    private static final By BILLING_LAST_NAME   = By.cssSelector("#BillingNewAddress_LastName");
    private static final By BILLING_EMAIL       = By.cssSelector("#BillingNewAddress_Email");
    private static final By BILLING_COUNTRY     = By.cssSelector("#BillingNewAddress_CountryId");
    private static final By BILLING_STATE       = By.cssSelector("#BillingNewAddress_StateProvinceId");
    private static final By BILLING_CITY        = By.cssSelector("#BillingNewAddress_City");
    private static final By BILLING_ADDRESS1    = By.cssSelector("#BillingNewAddress_Address1");
    private static final By BILLING_ZIP         = By.cssSelector("#BillingNewAddress_ZipPostalCode");
    private static final By BILLING_PHONE       = By.cssSelector("#BillingNewAddress_PhoneNumber");
    private static final By BILLING_CONTINUE    = By.cssSelector("#billing-buttons-container input[value='Continue']");

    // --- Shipping section ---
    private static final By SHIPPING_ADDRESS_DROPDOWN = By.cssSelector("#shipping-address-select");
    private static final By SHIPPING_FIRST_NAME = By.cssSelector("#ShippingNewAddress_FirstName");
    private static final By SHIPPING_LAST_NAME  = By.cssSelector("#ShippingNewAddress_LastName");
    private static final By SHIPPING_EMAIL      = By.cssSelector("#ShippingNewAddress_Email");
    private static final By SHIPPING_COUNTRY    = By.cssSelector("#ShippingNewAddress_CountryId");
    private static final By SHIPPING_STATE      = By.cssSelector("#ShippingNewAddress_StateProvinceId");
    private static final By SHIPPING_CITY       = By.cssSelector("#ShippingNewAddress_City");
    private static final By SHIPPING_ADDRESS1   = By.cssSelector("#ShippingNewAddress_Address1");
    private static final By SHIPPING_ZIP        = By.cssSelector("#ShippingNewAddress_ZipPostalCode");
    private static final By SHIPPING_PHONE      = By.cssSelector("#ShippingNewAddress_PhoneNumber");
    private static final By SHIPPING_CONTINUE   = By.cssSelector("#shipping-buttons-container input[value='Continue']");

    // --- Shipping-method section ---
    private static final By SHIPPING_METHOD_RADIO    = By.cssSelector("input[name='shippingoption']");
    private static final By SHIPPING_METHOD_CONTINUE = By.cssSelector("#shipping-method-buttons-container input[value='Continue']");

    // --- Payment-method section ---
    private static final By PAYMENT_METHOD_RADIO    = By.cssSelector("input[name='paymentmethod']");
    private static final By PAYMENT_METHOD_CONTINUE = By.cssSelector("#payment-method-buttons-container input[value='Continue']");

    // --- Payment-info section ---
    private static final By CARD_TYPE_DROPDOWN       = By.cssSelector("#CreditCardType");
    private static final By CARDHOLDER_NAME          = By.cssSelector("#CardholderName");
    private static final By CARD_NUMBER              = By.cssSelector("#CardNumber");
    private static final By EXPIRY_MONTH             = By.cssSelector("#ExpireMonth");
    private static final By EXPIRY_YEAR              = By.cssSelector("#ExpireYear");
    private static final By CVV_INPUT                = By.cssSelector("#CardCode");
    private static final By PAYMENT_INFO_CONTINUE    = By.cssSelector("#payment-info-buttons-container input[value='Continue']");

    // --- Confirm-order section ---
    private static final By CONFIRM_ORDER_BUTTON   = By.cssSelector("#confirm-order-buttons-container input[value='Confirm']");
    private static final By ORDER_COMPLETED_SECTION = By.cssSelector(".section.order-completed");

    // XPath templates for radio groups — each option radio is identified by its adjacent text label.
    private static final String SHIPPING_METHOD_XPATH =
            "//input[@name='shippingoption'][following-sibling::label[contains(normalize-space(.),'%s')]]";
    private static final String PAYMENT_METHOD_XPATH =
            "//input[@name='paymentmethod'][following-sibling::label[contains(normalize-space(.),'%s')]]";

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    private static By shippingMethodLocator(String methodName) {
        return By.xpath(String.format(SHIPPING_METHOD_XPATH, methodName));
    }

    private static By paymentMethodLocator(String methodName) {
        return By.xpath(String.format(PAYMENT_METHOD_XPATH, methodName));
    }

    /** Returns true when the billing address dropdown is visible. */
    public boolean isLoaded() {
        return actions.isDisplayed(BILLING_ADDRESS_DROPDOWN);
    }

    /**
     * Selects "New Address" in the billing dropdown, fills all address fields,
     * clicks Continue, and waits for the shipping section to become active.
     * Calls {@code wait.forClickable} on the state dropdown after selecting
     * country to allow the AJAX-populated state options to load.
     */
    public void fillBillingAddress(BillingAddress address) {
        actions.selectByVisibleText(BILLING_ADDRESS_DROPDOWN, "New Address", "billing address dropdown");
        actions.type(BILLING_FIRST_NAME, address.getFirstName(), "billing first name");
        actions.type(BILLING_LAST_NAME,  address.getLastName(),  "billing last name");
        actions.type(BILLING_EMAIL,      address.getEmail(),     "billing email");
        actions.selectByVisibleText(BILLING_COUNTRY, address.getCountry(), "billing country");
        wait.forClickable(BILLING_STATE);
        actions.selectByVisibleText(BILLING_STATE, address.getState(), "billing state");
        actions.type(BILLING_CITY,     address.getCity(),     "billing city");
        actions.type(BILLING_ADDRESS1, address.getAddress1(), "billing address1");
        actions.type(BILLING_ZIP,      address.getZip(),      "billing zip");
        actions.type(BILLING_PHONE,    address.getPhone(),    "billing phone");
        actions.click(BILLING_CONTINUE, "billing continue");
        wait.forVisible(SHIPPING_ADDRESS_DROPDOWN);
    }

    /**
     * Selects "New Address" in the shipping dropdown and refills all fields
     * (no ship-to-same shortcut in DOM — always refill independently), clicks
     * Continue, and waits for the shipping-method section to become active.
     */
    public void fillShippingAddress(BillingAddress address) {
        actions.selectByVisibleText(SHIPPING_ADDRESS_DROPDOWN, "New Address", "shipping address dropdown");
        actions.type(SHIPPING_FIRST_NAME, address.getFirstName(), "shipping first name");
        actions.type(SHIPPING_LAST_NAME,  address.getLastName(),  "shipping last name");
        actions.type(SHIPPING_EMAIL,      address.getEmail(),     "shipping email");
        actions.selectByVisibleText(SHIPPING_COUNTRY, address.getCountry(), "shipping country");
        wait.forClickable(SHIPPING_STATE);
        actions.selectByVisibleText(SHIPPING_STATE, address.getState(), "shipping state");
        actions.type(SHIPPING_CITY,     address.getCity(),     "shipping city");
        actions.type(SHIPPING_ADDRESS1, address.getAddress1(), "shipping address1");
        actions.type(SHIPPING_ZIP,      address.getZip(),      "shipping zip");
        actions.type(SHIPPING_PHONE,    address.getPhone(),    "shipping phone");
        actions.click(SHIPPING_CONTINUE, "shipping continue");
        wait.forVisible(SHIPPING_METHOD_RADIO);
    }

    /**
     * Selects the shipping option whose label contains {@code methodName}
     * (e.g. "Ground (0.00)"), clicks Continue, and waits for the
     * payment-method section.
     */
    public void selectShippingMethod(String methodName) {
        actions.click(shippingMethodLocator(methodName), "shipping method: " + methodName);
        actions.click(SHIPPING_METHOD_CONTINUE, "shipping method continue");
        wait.forVisible(PAYMENT_METHOD_RADIO);
    }

    /**
     * Selects the payment option whose label contains {@code methodName}
     * (e.g. "Credit Card"), clicks Continue, and waits for the credit-card
     * type dropdown to confirm the payment-info section is active.
     */
    public void selectPaymentMethod(String methodName) {
        actions.click(paymentMethodLocator(methodName), "payment method: " + methodName);
        actions.click(PAYMENT_METHOD_CONTINUE, "payment method continue");
        wait.forVisible(CARD_TYPE_DROPDOWN);
    }

    /**
     * Fills the credit-card form (type, holder, number, expiry, CVV), clicks
     * Continue, and waits for the Confirm button to confirm the section is active.
     */
    public void fillPaymentInfo(CreditCard card) {
        actions.selectByVisibleText(CARD_TYPE_DROPDOWN, card.getCardType(), "card type");
        actions.type(CARDHOLDER_NAME, card.getCardholder(), "cardholder name");
        actions.type(CARD_NUMBER,     card.getCardNumber(), "card number");
        actions.selectByVisibleText(EXPIRY_MONTH, card.getExpiryMonth(), "expiry month");
        actions.selectByVisibleText(EXPIRY_YEAR,  card.getExpiryYear(),  "expiry year");
        actions.type(CVV_INPUT, card.getCvv(), "cvv");
        actions.click(PAYMENT_INFO_CONTINUE, "payment info continue");
        wait.forVisible(CONFIRM_ORDER_BUTTON);
    }

    /**
     * Clicks the Confirm button and waits for the order-completed section
     * before returning OrderConfirmationPage. Waiting on the section element
     * is more reliable than waiting for a URL change (which races with redirect).
     */
    public OrderConfirmationPage confirmOrder() {
        actions.click(CONFIRM_ORDER_BUTTON, "confirm order button");
        wait.forVisible(ORDER_COMPLETED_SECTION);
        return new OrderConfirmationPage(driver);
    }
}
