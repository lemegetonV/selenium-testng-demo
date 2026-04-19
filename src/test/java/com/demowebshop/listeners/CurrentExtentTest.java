package com.demowebshop.listeners;

import com.aventstack.extentreports.ExtentTest;

/**
 * Bridge between {@link ExtentReportListener} and {@link ExtentAppender}.
 *
 * <p>The listener publishes the active ExtentTest node here on
 * {@code onTestStart}; the Log4j2 appender reads it on every log event so
 * SLF4J output is forwarded into the report. Decoupling them via this
 * holder avoids a hard dependency between the two — the appender has no
 * knowledge of the listener's internal state.
 */
public final class CurrentExtentTest {

    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    private CurrentExtentTest() {}

    public static void set(ExtentTest test) {
        CURRENT.set(test);
    }

    public static ExtentTest get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
