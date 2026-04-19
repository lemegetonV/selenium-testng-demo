package com.demowebshop.core;

import com.demowebshop.utils.WaitUtils;
import org.openqa.selenium.WebDriver;

/**
 * Parent of every page object. Provides {@link ElementActions} and
 * {@link WaitUtils} so subclasses never construct their own — keeps the
 * 3-tier click fallback, screenshot capture, and wait timeouts consistent
 * across pages.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final ElementActions actions;
    protected final WaitUtils wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
        this.wait = new WaitUtils(driver);
    }
}
