package com.demowebshop.models;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class SearchTerm {

    private final String term;
    private final String expectedProductName;
}
