package com.demowebshop.pages;

import com.demowebshop.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;

/** Shopping cart — line-item inspection, terms-of-service tick, and checkout entry. */
public class CartPage extends BasePage {

    private static final By CHECKOUT_BUTTON    = By.cssSelector("#checkout");
    private static final By TERMS_OF_SERVICE   = By.cssSelector("#termsofservice");
    private static final By PRODUCT_NAME_LINK  =
            By.xpath("//td[contains(@class,'product')]//a[contains(@class,'product-name')]");
    // Billing dropdown is the first visible element on CheckoutPage — used as a loaded signal.
    private static final By BILLING_ADDRESS_DROPDOWN = By.cssSelector("#billing-address-select");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    /** Returns true when the Checkout button is visible. */
    public boolean isLoaded() {
        return actions.isDisplayed(CHECKOUT_BUTTON);
    }

    /** Returns the display names of all products currently in the cart. */
    public List<String> getCartItemNames() {
        return actions.findAllTexts(PRODUCT_NAME_LINK, "cart product names");
    }

    /** Ticks the terms-of-service checkbox; does nothing if already ticked. */
    public void acceptTermsOfService() {
        actions.check(TERMS_OF_SERVICE, "terms of service checkbox");
    }

    /** Clicks the Checkout button and waits for the billing form to appear. */
    public CheckoutPage proceedToCheckout() {
        actions.click(CHECKOUT_BUTTON, "checkout button");
        wait.forVisible(BILLING_ADDRESS_DROPDOWN);
        return new CheckoutPage(driver);
    }

    /** Accepts terms of service then proceeds to checkout — preferred entry point for tests. */
    public CheckoutPage checkout() {
        acceptTermsOfService();
        return proceedToCheckout();
    }
}
