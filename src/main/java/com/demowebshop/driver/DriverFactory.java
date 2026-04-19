package com.demowebshop.driver;

import com.demowebshop.config.ConfigReader;
import com.demowebshop.core.FrameworkException;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

/**
 * Per-thread {@link WebDriver} provisioning. {@link ThreadLocal} keeps the
 * door open for parallel execution later even though Prompt 02 ships
 * sequential-only.
 *
 * <p>WebDriverManager handles the binary; we deliberately do <b>not</b> call
 * {@code System.setProperty("webdriver.chrome.driver", …)} afterward because
 * it can override Selenium Manager with a stale binary (PLAN.md §2,
 * Amendment 7).
 */
@Slf4j
public final class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
        // static utility — no instances
    }

    public static void initDriver(String browser) {
        if (browser == null || browser.isBlank()) {
            throw new FrameworkException("Browser name is required to initialize a driver");
        }
        String normalized = browser.trim().toLowerCase();

        if (!"chrome".equals(normalized)) {
            throw new FrameworkException("Unsupported browser: " + browser
                    + ". Prompt 02 ships Chrome only — see PLAN.md §6.");
        }

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = buildChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        int pageLoadTimeout = ConfigReader.getInt("pageLoadTimeoutSeconds", 30);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));

        DRIVER.set(driver);
        log.info("Initialized Chrome driver (headless={}, pageLoadTimeoutSeconds={})",
                ConfigReader.getBoolean("headless", false), pageLoadTimeout);
    }

    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new FrameworkException(
                    "WebDriver was requested before initDriver() — check BaseTest @BeforeMethod ordering");
        }
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (Exception ex) {
            log.warn("Driver quit raised an exception (ignored): {}", ex.getMessage());
        } finally {
            DRIVER.remove();
        }
    }

    /**
     * Builds the Chrome options bundle. Always-on flags suppress notification
     * popups, allow CDP origins, and lock window size for layout stability.
     * Headless-only flags add the new headless mode plus the extras CI
     * environments typically need (no GPU, no sandbox).
     */
    private static ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--disable-notifications",
                "--remote-allow-origins=*",
                "--window-size=1920,1080");

        if (ConfigReader.getBoolean("headless", false)) {
            options.addArguments(
                    "--headless=new",
                    "--disable-gpu",
                    "--no-sandbox");
        }
        return options;
    }
}
