package com.demowebshop.core;

import com.demowebshop.utils.ScreenshotUtils;
import com.demowebshop.utils.WaitUtils;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import java.util.Locale;

/**
 * The single entry point for every DOM interaction in the framework. Page
 * objects route through here so the click-fallback chain, stale-element
 * retry, password masking, and uniform logging only live in one place.
 *
 * <p>Click strategy is a 3-tier fallback: native click first; if it is
 * intercepted or the element refuses to accept input, scroll into view and
 * retry; if that still fails, fall back to a JavaScript click. Native and
 * scroll-retry success log at DEBUG; the JS fallback logs at WARN so
 * structural locator problems show up in the run log instead of being
 * silently masked.
 */
@Slf4j
public class ElementActions {

    private static final String[] PASSWORD_HINTS = {"password", "cvv", "card number", "credit card"};

    private final WebDriver driver;
    private final WaitUtils wait;
    private final JavascriptExecutor js;
    private final Actions actions;

    public ElementActions(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
        this.js = (JavascriptExecutor) driver;
        this.actions = new Actions(driver);
    }

    /**
     * Click with the 3-tier fallback. Wrapped in a single full-chain retry
     * for {@link StaleElementReferenceException} — re-resolving the locator
     * usually clears it; failing twice means the page genuinely is in a
     * bad state.
     */
    public void click(By locator, String description) {
        log.info("Click: {} [{}]", description, locator);
        try {
            clickWithFallback(locator, description);
        } catch (StaleElementReferenceException stale) {
            log.debug("Stale element on click '{}', retrying once", description);
            clickWithFallback(locator, description);
        } catch (FrameworkException ex) {
            throw ex;
        } catch (Exception ex) {
            failure("click '" + description + "'", ex);
        }
    }

    public void type(By locator, String text, String description) {
        String shown = isSensitive(description) ? "****" : text;
        log.info("Type '{}' into: {} [{}]", shown, description, locator);
        try {
            WebElement element = wait.forVisible(locator);
            element.clear();
            element.sendKeys(text);
        } catch (FrameworkException ex) {
            throw ex;
        } catch (Exception ex) {
            failure("type into '" + description + "'", ex);
        }
    }

    public void selectByVisibleText(By locator, String visibleText, String description) {
        log.info("Select by text '{}': {} [{}]", visibleText, description, locator);
        try {
            new Select(wait.forVisible(locator)).selectByVisibleText(visibleText);
        } catch (FrameworkException ex) {
            throw ex;
        } catch (Exception ex) {
            failure("selectByVisibleText '" + description + "'", ex);
        }
    }

    public void selectByValue(By locator, String value, String description) {
        log.info("Select by value '{}': {} [{}]", value, description, locator);
        try {
            new Select(wait.forVisible(locator)).selectByValue(value);
        } catch (FrameworkException ex) {
            throw ex;
        } catch (Exception ex) {
            failure("selectByValue '" + description + "'", ex);
        }
    }

    public String getText(By locator, String description) {
        log.info("Read text: {} [{}]", description, locator);
        try {
            return wait.forVisible(locator).getText();
        } catch (FrameworkException ex) {
            throw ex;
        } catch (Exception ex) {
            failure("getText '" + description + "'", ex);
            return "";
        }
    }

    /**
     * Visibility check that swallows {@link NoSuchElementException} only.
     * Other exceptions propagate so genuine driver failures aren't masked.
     * No screenshot on miss — this is a query, not an action.
     */
    public boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException ex) {
            return false;
        }
    }

    public boolean isPresent(By locator) {
        try {
            return !driver.findElements(locator).isEmpty();
        } catch (Exception ex) {
            return false;
        }
    }

    public void waitForText(By locator, String expectedText) {
        log.info("Wait for text '{}' in: {}", expectedText, locator);
        wait.forText(locator, expectedText);
    }

    public void scrollIntoView(By locator) {
        log.debug("Scroll into view: {}", locator);
        WebElement element = driver.findElement(locator);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    public void hover(By locator, String description) {
        log.info("Hover: {} [{}]", description, locator);
        try {
            WebElement element = wait.forVisible(locator);
            actions.moveToElement(element).perform();
        } catch (FrameworkException ex) {
            throw ex;
        } catch (Exception ex) {
            failure("hover '" + description + "'", ex);
        }
    }

    private void clickWithFallback(By locator, String description) {
        WebElement element = wait.forClickable(locator);
        try {
            element.click();
            log.debug("Native click succeeded for '{}'", description);
            return;
        } catch (ElementNotInteractableException intercepted) {
            log.debug("Native click intercepted for '{}', scrolling into view", description);
        }

        scrollIntoView(locator);
        WebElement rebound = wait.forClickable(locator);
        try {
            rebound.click();
            log.debug("Scroll + native click succeeded for '{}'", description);
            return;
        } catch (ElementNotInteractableException stillIntercepted) {
            log.debug("Scroll + native click still intercepted for '{}', falling back to JS", description);
        }

        WebElement jsTarget = driver.findElement(locator);
        js.executeScript("arguments[0].click();", jsTarget);
        // WARN so frequent JS fallbacks surface in logs as a structural smell, not just flakiness.
        log.warn("JS click fallback used for '{}' [{}]", description, locator);
    }

    private void failure(String action, Exception cause) {
        ScreenshotUtils.captureToFile(action);
        log.error("Failed to {}: {}", action, cause.getMessage());
        throw new FrameworkException("Failed to " + action, cause);
    }

    private static boolean isSensitive(String description) {
        if (description == null) {
            return false;
        }
        String lower = description.toLowerCase(Locale.ROOT);
        for (String hint : PASSWORD_HINTS) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        return false;
    }
}
