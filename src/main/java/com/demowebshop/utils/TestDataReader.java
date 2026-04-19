package com.demowebshop.utils;

import com.demowebshop.core.FrameworkException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

/**
 * Loads JSON test data from {@code src/test/resources/testdata/} via the
 * classpath and deserializes into POJOs through Jackson.
 */
public final class TestDataReader {

    private static final String TESTDATA_ROOT = "testdata/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestDataReader() {
        // static utility — no instances
    }

    public static <T> T load(String filename, Class<T> type) {
        String resource = TESTDATA_ROOT + filename;
        try (InputStream stream = TestDataReader.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new FrameworkException("Test data file not found on classpath: " + resource);
            }
            return MAPPER.readValue(stream, type);
        } catch (FrameworkException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FrameworkException("Failed to deserialize " + resource + " into " + type.getSimpleName(), ex);
        }
    }
}
