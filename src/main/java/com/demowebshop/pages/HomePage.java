package com.demowebshop.pages;

import com.demowebshop.config.ConfigReader;
import com.demowebshop.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Landing page — navigate to base URL and confirm the site loaded. */
public class HomePage extends BasePage {

    private static final By HOME_LANDMARK = By.cssSelector(".header-logo");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    /** Navigates to the configured {@code baseUrl} and waits for the header to appear. */
    public void open() {
        driver.get(ConfigReader.getString("baseUrl"));
        wait.forVisible(HOME_LANDMARK);
    }

    /** Returns true when the header logo is visible. */
    public boolean isLoaded() {
        return actions.isDisplayed(HOME_LANDMARK);
    }
}
