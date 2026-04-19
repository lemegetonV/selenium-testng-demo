package com.demowebshop.core;

/**
 * Unchecked exception raised by framework code (driver, waits, element actions,
 * config loading). Distinct from {@link AssertionError} so test failures and
 * framework misconfiguration are easy to tell apart in stack traces.
 */
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
