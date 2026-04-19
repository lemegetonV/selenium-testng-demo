package com.demowebshop.pages;

import com.demowebshop.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Order confirmation page — success banner and order number. */
public class OrderConfirmationPage extends BasePage {

    private static final By SUCCESS_BANNER_TITLE  = By.cssSelector(".section.order-completed .title");
    private static final By ORDER_NUMBER_CONTAINER = By.cssSelector(".section.order-completed");
    private static final By CONTINUE_BUTTON       = By.cssSelector(".order-completed input[value='Continue']");

    /** Matches "Order number: 2276297" — the number is plain inline text, not in a dedicated element. */
    private static final Pattern ORDER_NUMBER_PATTERN = Pattern.compile("Order number:\\s*(\\d+)");

    public OrderConfirmationPage(WebDriver driver) {
        super(driver);
    }

    /** Returns true when the order-completed section is visible. */
    public boolean isLoaded() {
        return actions.isDisplayed(ORDER_NUMBER_CONTAINER);
    }

    /** Returns true when the success banner contains the expected confirmation text. */
    public boolean isOrderPlaced() {
        return actions.getText(SUCCESS_BANNER_TITLE, "order success banner")
                .contains("Your order has been successfully processed!");
    }

    /**
     * Extracts the numeric order number from the confirmation section text.
     * Returns an empty string if the pattern is not found — assertion is the
     * caller's responsibility.
     */
    public String getOrderNumber() {
        String text = actions.getText(ORDER_NUMBER_CONTAINER, "order number container");
        Matcher matcher = ORDER_NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /** Clicks the Continue button and returns to the home page. */
    public HomePage continueShopping() {
        actions.click(CONTINUE_BUTTON, "continue shopping button");
        return new HomePage(driver);
    }
}
