package com.demowebshop.models;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * A known-good Luhn-valid card number paired with its card type label exactly
 * as the checkout payment dropdown renders it. Loaded from paymentCards.json.
 */
@Data
@Builder
@Jacksonized
public class TestCard {

    private final String cardType;
    private final String cardNumber;
}
