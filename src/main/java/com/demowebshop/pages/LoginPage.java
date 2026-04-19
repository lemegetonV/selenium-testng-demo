package com.demowebshop.pages;

import com.demowebshop.core.BasePage;
import com.demowebshop.models.User;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Login page — email/password form and error message surface. */
public class LoginPage extends BasePage {

    private static final By EMAIL_INPUT  = By.cssSelector("#Email");
    private static final By PASSWORD_INPUT = By.cssSelector("#Password");
    private static final By LOGIN_BUTTON = By.cssSelector("input[value='Log in']");
    private static final By LOGIN_ERROR  = By.cssSelector(".message-error");
    private static final By LOGOUT_LINK  = By.cssSelector(".ico-logout");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Fills the email and password fields, submits the form, and waits for
     * the logout link to confirm a successful login before returning HomePage.
     */
    public HomePage login(User user) {
        actions.type(EMAIL_INPUT, user.getEmail(), "email");
        actions.type(PASSWORD_INPUT, user.getPassword(), "password");
        actions.click(LOGIN_BUTTON, "login button");
        wait.forVisible(LOGOUT_LINK);
        return new HomePage(driver);
    }

    /** Returns true when the email input is visible. */
    public boolean isLoaded() {
        return actions.isDisplayed(EMAIL_INPUT);
    }

    /** Returns the login error text, or an empty string when no error is shown. */
    public String getErrorMessage() {
        if (!actions.isPresent(LOGIN_ERROR)) {
            return "";
        }
        return actions.getText(LOGIN_ERROR, "login error message");
    }
}
