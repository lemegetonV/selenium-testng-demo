package com.demowebshop.dataproviders;

import com.demowebshop.models.SearchTerm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.DataProvider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Supplies search-term rows to data-driven tests. Reads
 * {@code testdata/products.json} and converts each element of the
 * {@code searchTerms} array to a {@link SearchTerm} row.
 */
public class ProductDataProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @DataProvider(name = "searchTerms")
    public static Object[][] searchTerms() throws Exception {
        try (InputStream stream = ProductDataProvider.class
                .getClassLoader()
                .getResourceAsStream("testdata/products.json")) {

            JsonNode root = MAPPER.readTree(stream);
            JsonNode terms = root.get("searchTerms");

            List<Object[]> rows = new ArrayList<>();
            for (JsonNode node : terms) {
                SearchTerm term = MAPPER.treeToValue(node, SearchTerm.class);
                rows.add(new Object[]{term});
            }
            return rows.toArray(new Object[0][]);
        }
    }
}
