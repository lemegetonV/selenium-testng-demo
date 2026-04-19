package com.demowebshop.utils;

import com.demowebshop.config.ConfigReader;
import com.demowebshop.core.FrameworkException;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Thin wrapper over {@link WebDriverWait}. Centralizes timeout handling so
 * {@code explicitWaitSeconds} from config is the single knob, and so wait
 * timeouts surface as {@link FrameworkException} rather than raw
 * {@link TimeoutException} (which is too easy to confuse with a failed
 * assertion).
 */
public final class WaitUtils {

    private final WebDriverWait wait;

    public WaitUtils(WebDriver driver) {
        int seconds = ConfigReader.getInt("explicitWaitSeconds", 15);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    public WebElement forVisible(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException ex) {
            throw new FrameworkException("Timed out waiting for element to be visible: " + locator, ex);
        }
    }

    public WebElement forClickable(By locator) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException ex) {
            throw new FrameworkException("Timed out waiting for element to be clickable: " + locator, ex);
        }
    }

    public boolean forInvisible(By locator) {
        try {
            return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (TimeoutException ex) {
            throw new FrameworkException("Timed out waiting for element to disappear: " + locator, ex);
        }
    }

    public boolean forText(By locator, String expectedText) {
        try {
            return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, expectedText));
        } catch (TimeoutException ex) {
            throw new FrameworkException(
                    "Timed out waiting for text '" + expectedText + "' in element: " + locator, ex);
        }
    }

    public boolean forUrlContains(String fragment) {
        try {
            return wait.until(ExpectedConditions.urlContains(fragment));
        } catch (TimeoutException ex) {
            throw new FrameworkException("Timed out waiting for URL to contain '" + fragment + "'", ex);
        }
    }
}
