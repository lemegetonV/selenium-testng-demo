package com.demowebshop.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreditCard {

    private final String cardholder;
    private final String cardNumber;
    private final String cardType;
    private final String expiryMonth;
    private final String expiryYear;
    private final String cvv;
}
