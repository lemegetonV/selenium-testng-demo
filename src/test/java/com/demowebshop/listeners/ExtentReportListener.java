package com.demowebshop.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.demowebshop.config.ConfigReader;
import com.demowebshop.utils.ScreenshotUtils;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Hooks into TestNG's listener chain to produce an ExtentReports HTML report
 * at {@code reports/extent-report.html}. Screenshots on failure are both
 * base64-embedded in the HTML (for self-contained sharing) and copied to disk
 * (for CI artifact upload).
 *
 * <p>ThreadLocal keeps the door open for parallel runs later even though this
 * suite ships sequential-only.
 */
@Slf4j
public class ExtentReportListener implements ITestListener {

    private static final String REPORT_PATH = "reports/extent-report.html";

    private ExtentReports extent;
    private final ThreadLocal<ExtentTest> testNode = new ThreadLocal<>();

    @Override
    public void onStart(ITestContext context) {
        ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_PATH);
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setDocumentTitle("DemoWebShop Test Report");
        spark.config().setReportName("End-to-End Purchase Suite");

        extent = new ExtentReports();
        extent.attachReporter(spark);

        extent.setSystemInfo("Browser", ConfigReader.getString("browser", "chrome"));
        extent.setSystemInfo("BaseUrl", ConfigReader.getString("baseUrl", "https://demowebshop.tricentis.com"));
        extent.setSystemInfo("Environment", ConfigReader.getString("environment", "prod"));

        log.info("ExtentReports initialized — output: {}", REPORT_PATH);
    }

    @Override
    public void onTestStart(ITestResult result) {
        ExtentTest test = extent.createTest(result.getMethod().getMethodName());
        testNode.set(test);
        CurrentExtentTest.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        testNode.get().pass("Test passed");
        CurrentExtentTest.clear();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = testNode.get();
        test.fail(result.getThrowable());

        String base64 = ScreenshotUtils.captureBase64();
        if (!base64.isBlank()) {
            test.fail(MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
        }

        String filePath = ScreenshotUtils.captureToFile(result.getMethod().getMethodName());
        if (!filePath.isBlank()) {
            log.info("Screenshot saved to {}", filePath);
        }
        CurrentExtentTest.clear();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String reason = result.getThrowable() != null
                ? result.getThrowable().getMessage()
                : "No skip reason available";
        testNode.get().skip(reason);
        CurrentExtentTest.clear();
    }

    @Override
    public void onFinish(ITestContext context) {
        if (extent != null) {
            extent.flush();
            log.info("ExtentReports flushed to {}", REPORT_PATH);
        }
        testNode.remove();
        CurrentExtentTest.clear();
    }
}
