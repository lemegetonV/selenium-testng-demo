package com.demowebshop.utils;

import com.demowebshop.core.FrameworkException;
import com.demowebshop.driver.DriverFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Screenshot capture for failure evidence. Two output modes:
 * base64 (embedded directly in ExtentReports HTML so the report file is
 * self-contained) and on-disk PNG (for CI artifact upload).
 */
@Slf4j
public final class ScreenshotUtils {

    private static final Path SCREENSHOT_DIR = Paths.get("reports", "screenshots");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtils() {
        // static utility — no instances
    }

    public static String captureBase64() {
        try {
            WebDriver driver = DriverFactory.getDriver();
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception ex) {
            log.warn("Base64 screenshot capture failed: {}", ex.getMessage());
            return "";
        }
    }

    /**
     * Saves a PNG under {@code reports/screenshots/} using a sanitized
     * filename. Returns the absolute path written, or empty string on
     * failure (we never want screenshot capture to mask the real test
     * failure).
     */
    public static String captureToFile(String description) {
        try {
            ensureDirectory();
            WebDriver driver = DriverFactory.getDriver();
            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String filename = sanitize(description) + "_" + LocalDateTime.now().format(TIMESTAMP) + ".png";
            File target = SCREENSHOT_DIR.resolve(filename).toFile();
            FileUtils.copyFile(source, target);
            return target.getAbsolutePath();
        } catch (Exception ex) {
            log.warn("File screenshot capture failed for '{}': {}", description, ex.getMessage());
            return "";
        }
    }

    /**
     * Wipes {@code reports/screenshots/} so a committed run is always clean.
     * Called from {@code BaseTest @BeforeSuite}. Preserves {@code .gitkeep}
     * so the folder stays tracked across clean runs. Missing directory is
     * fine — we recreate it on next capture.
     */
    public static void wipeScreenshotsFolder() {
        File directory = SCREENSHOT_DIR.toFile();
        if (!directory.exists()) {
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        try {
            for (File child : children) {
                if (".gitkeep".equals(child.getName())) {
                    continue;
                }
                if (child.isDirectory()) {
                    FileUtils.deleteDirectory(child);
                } else if (!child.delete()) {
                    throw new IOException("Could not delete " + child.getAbsolutePath());
                }
            }
            log.info("Wiped {} for the new run", SCREENSHOT_DIR);
        } catch (IOException ex) {
            throw new FrameworkException("Failed to wipe " + SCREENSHOT_DIR, ex);
        }
    }

    private static void ensureDirectory() {
        File directory = SCREENSHOT_DIR.toFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new FrameworkException("Could not create " + SCREENSHOT_DIR);
        }
    }

    private static String sanitize(String description) {
        if (description == null || description.isBlank()) {
            return "screenshot";
        }
        return description.trim().replaceAll("[^A-Za-z0-9]+", "_");
    }
}
