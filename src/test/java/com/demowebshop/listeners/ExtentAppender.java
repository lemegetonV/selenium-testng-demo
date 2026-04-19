package com.demowebshop.listeners;

import com.aventstack.extentreports.ExtentTest;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;

/**
 * Custom Log4j2 appender that forwards SLF4J log events into the active
 * ExtentReports test node. Reads the current node from
 * {@link CurrentExtentTest}; events emitted outside a test (e.g. during
 * {@code @BeforeSuite}) have no active node and are dropped silently.
 *
 * <p>Level mapping: INFO → info, WARN → warning, ERROR → fail.
 * DEBUG/TRACE are intentionally skipped so the report stays focused on
 * the user-visible narrative — file/console appenders still capture them.
 */
@Plugin(name = "ExtentAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class ExtentAppender extends AbstractAppender {

    protected ExtentAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout, true, null);
    }

    @PluginFactory
    public static ExtentAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout) {
        return new ExtentAppender(name, filter, layout);
    }

    @Override
    public void append(LogEvent event) {
        ExtentTest test = CurrentExtentTest.get();
        if (test == null) {
            return;
        }

        String message = event.getMessage().getFormattedMessage();

        switch (event.getLevel().getStandardLevel()) {
            case INFO  -> test.info(message);
            case WARN  -> test.warning(message);
            case ERROR, FATAL -> test.fail(message);
            default -> { /* DEBUG/TRACE intentionally dropped to keep the report focused */ }
        }
    }
}
