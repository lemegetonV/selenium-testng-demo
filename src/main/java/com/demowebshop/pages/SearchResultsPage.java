package com.demowebshop.pages;

import com.demowebshop.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Search results grid — verify product presence and add to cart directly from the tile. */
public class SearchResultsPage extends BasePage {

    private static final By PRODUCT_GRID = By.cssSelector(".product-title a");
    private static final By ADD_TO_CART_NOTIFICATION = By.cssSelector("#bar-notification");

    /**
     * XPath targeting the Add to Cart input on the tile matching an exact product name.
     * Traverses: product-title link → ancestor product-item div → input in that scope.
     */
    private static final String ADD_TO_CART_XPATH =
            "//h2[contains(@class,'product-title')]/a[normalize-space()='%s']" +
            "/ancestor::div[contains(@class,'product-item')]//input[@value='Add to cart']";

    private static final String PRODUCT_TILE_XPATH =
            "//h2[contains(@class,'product-title')]/a[normalize-space()='%s']";

    private static By addToCartLocator(String productName) {
        return By.xpath(String.format(ADD_TO_CART_XPATH, productName));
    }

    private static By productTileLocator(String productName) {
        return By.xpath(String.format(PRODUCT_TILE_XPATH, productName));
    }

    public SearchResultsPage(WebDriver driver) {
        super(driver);
    }

    /** Returns true when at least one product title is visible in the results grid. */
    public boolean isLoaded() {
        return actions.isDisplayed(PRODUCT_GRID);
    }

    /** Returns true if a tile with the exact {@code productName} exists on the page. */
    public boolean containsProduct(String productName) {
        return actions.isPresent(productTileLocator(productName));
    }

    /**
     * Clicks Add to Cart on the tile matching {@code productName} and waits for
     * the notification bar to confirm the item was queued. Does not wait for the
     * notification to disappear — it may fade before a follow-up wait could catch it.
     */
    public void addProductToCart(String productName) {
        actions.click(addToCartLocator(productName), "add to cart: " + productName);
        wait.forVisible(ADD_TO_CART_NOTIFICATION);
    }
}
