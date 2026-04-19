package com.demowebshop.models;

/**
 * Runtime capture of a product's name and price as seen on the search result
 * tile (or PDP). Not loaded from JSON — populated after DOM scraping so
 * CartPage can assert on the same values that were displayed at selection time.
 */
public record Product(String name, String price) {}
