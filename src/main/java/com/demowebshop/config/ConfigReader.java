package com.demowebshop.config;

import com.demowebshop.core.FrameworkException;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Properties;

/**
 * Loads {@code config.properties} from the classpath once and exposes typed
 * getters. {@code System.getProperty} always wins over the file value, so
 * {@code -Dbrowser=firefox} from the command line overrides the shipped
 * default without editing files.
 */
@Slf4j
public final class ConfigReader {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream stream = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (stream == null) {
                throw new FrameworkException("Could not locate " + CONFIG_FILE + " on the classpath");
            }
            PROPERTIES.load(stream);
            log.info("Loaded {} ({} keys)", CONFIG_FILE, PROPERTIES.size());
        } catch (FrameworkException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FrameworkException("Failed to load " + CONFIG_FILE, ex);
        }
    }

    private ConfigReader() {
        // static utility — no instances
    }

    public static String getString(String key) {
        String value = resolve(key);
        if (value == null) {
            throw new FrameworkException("Missing required config key: " + key);
        }
        return value;
    }

    public static String getString(String key, String defaultValue) {
        String value = resolve(key);
        return value != null ? value : defaultValue;
    }

    public static int getInt(String key) {
        String value = getString(key);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new FrameworkException("Config key '" + key + "' is not an int: " + value, ex);
        }
    }

    public static int getInt(String key, int defaultValue) {
        String value = resolve(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new FrameworkException("Config key '" + key + "' is not an int: " + value, ex);
        }
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key).trim());
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = resolve(key);
        return value != null ? Boolean.parseBoolean(value.trim()) : defaultValue;
    }

    private static String resolve(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        return PROPERTIES.getProperty(key);
    }
}
