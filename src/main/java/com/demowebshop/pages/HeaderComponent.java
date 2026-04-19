package com.demowebshop.pages;

import com.demowebshop.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Shared header present on every page — search, cart, and login/logout navigation. */
public class HeaderComponent extends BasePage {

    private static final By LOGIN_LINK  = By.cssSelector("a[href='/login']");
    private static final By LOGOUT_LINK = By.cssSelector(".ico-logout");
    private static final By SEARCH_BOX  = By.cssSelector("#small-searchterms");
    private static final By SEARCH_BUTTON = By.cssSelector("input.search-box-button");
    private static final By CART_LINK   = By.cssSelector("#topcartlink a");

    public HeaderComponent(WebDriver driver) {
        super(driver);
    }

    /** Clicks the Login link and returns the LoginPage. */
    public LoginPage clickLoginLink() {
        actions.click(LOGIN_LINK, "login link");
        return new LoginPage(driver);
    }

    /**
     * Types {@code term} into the search box, clicks the Search button, and
     * returns the SearchResultsPage.
     */
    public SearchResultsPage searchFor(String term) {
        actions.type(SEARCH_BOX, term, "search box");
        actions.click(SEARCH_BUTTON, "search button");
        return new SearchResultsPage(driver);
    }

    /** Clicks the shopping cart link and returns CartPage. */
    public CartPage openCart() {
        actions.click(CART_LINK, "cart link");
        return new CartPage(driver);
    }

    /** Clicks Log out and waits for the login link to confirm the session ended. */
    public HomePage clickLogout() {
        actions.click(LOGOUT_LINK, "logout link");
        wait.forVisible(LOGIN_LINK);
        return new HomePage(driver);
    }

    /** Returns true when the logout link is present — indicates an active session. */
    public boolean isLoggedIn() {
        return actions.isPresent(LOGOUT_LINK);
    }

    /**
     * Parses the cart item count from the cart link text "Shopping cart(N)".
     * Returns 0 when the text contains no numeric count (e.g. empty cart).
     */
    public int getCartItemCount() {
        String text = actions.getText(CART_LINK, "cart link");
        int openParen  = text.indexOf('(');
        int closeParen = text.indexOf(')');
        if (openParen < 0 || closeParen <= openParen) {
            return 0;
        }
        try {
            return Integer.parseInt(text.substring(openParen + 1, closeParen).trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
