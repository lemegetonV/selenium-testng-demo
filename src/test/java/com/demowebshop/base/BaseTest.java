package com.demowebshop.base;

import com.demowebshop.config.ConfigReader;
import com.demowebshop.driver.DriverFactory;
import com.demowebshop.utils.ScreenshotUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.lang.reflect.Method;

/**
 * Parent of every test class. Handles driver lifecycle, MDC wiring, and
 * suite-level setup. Reads {@code browser} and {@code baseUrl} from the
 * {@code testng.xml} suite parameters first; falls back to ConfigReader
 * values so command-line {@code -D} overrides still work.
 */
@Slf4j
public abstract class BaseTest {

    private static final String MDC_TEST_NAME = "testName";

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        ScreenshotUtils.wipeScreenshotsFolder();
        log.info("Suite started — screenshots folder wiped");
    }

    @Parameters({"browser", "baseUrl"})
    @BeforeMethod(alwaysRun = true)
    public void beforeMethod(
            @Optional String browser,
            @Optional String baseUrl,
            Method method) {

        String resolvedBrowser = resolve(browser, "browser", "chrome");
        String resolvedBaseUrl = resolve(baseUrl, "baseUrl", "https://demowebshop.tricentis.com");

        String testName = method.getName();
        ThreadContext.put(MDC_TEST_NAME, testName);
        log.info("Starting test: {} (browser={}, baseUrl={})", testName, resolvedBrowser, resolvedBaseUrl);

        DriverFactory.initDriver(resolvedBrowser);
        DriverFactory.getDriver().get(resolvedBaseUrl);
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        log.info("Finished test: {} — {}", result.getName(), statusLabel(result.getStatus()));
        DriverFactory.quitDriver();
        ThreadContext.remove(MDC_TEST_NAME);
    }

    protected WebDriver getDriver() {
        return DriverFactory.getDriver();
    }

    private static String resolve(String suiteParam, String configKey, String hardDefault) {
        if (suiteParam != null && !suiteParam.isBlank()) {
            return suiteParam;
        }
        return ConfigReader.getString(configKey, hardDefault);
    }

    private static String statusLabel(int status) {
        return switch (status) {
            case ITestResult.SUCCESS -> "PASSED";
            case ITestResult.FAILURE -> "FAILED";
            case ITestResult.SKIP -> "SKIPPED";
            default -> "UNKNOWN";
        };
    }
}
