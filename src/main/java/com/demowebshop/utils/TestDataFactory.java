package com.demowebshop.utils;

import com.demowebshop.config.ConfigReader;
import com.demowebshop.models.BillingAddress;
import com.demowebshop.models.CreditCard;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;

import java.util.Locale;
import java.util.Random;

/**
 * Datafaker-backed generator for billing/shipping/card payloads. Sequential
 * suite execution lets us share a single seeded {@link Faker} instance —
 * reproducible runs without any thread-local plumbing. Seed comes from
 * {@code dataFakerSeed} in config (overridable with {@code -DdataFakerSeed}).
 */
@Slf4j
public final class TestDataFactory {

    private static final Faker FAKER;

    static {
        long seed = ConfigReader.getInt("dataFakerSeed", 42);
        FAKER = new Faker(Locale.US, new Random(seed));
        log.info("TestDataFactory seeded with dataFakerSeed={}", seed);
    }

    private TestDataFactory() {
        // static utility — no instances
    }

    public static BillingAddress generateBillingAddress() {
        return BillingAddress.builder()
                .firstName(FAKER.name().firstName())
                .lastName(FAKER.name().lastName())
                .email(FAKER.internet().emailAddress())
                .country("United States")
                .city(FAKER.address().city())
                .address1(FAKER.address().streetAddress())
                .zip(FAKER.address().zipCode())
                .phone(generatePhoneNumber())
                .build();
    }

    public static CreditCard generateCreditCard() {
        return CreditCard.builder()
                .cardholder(FAKER.name().fullName())
                .cardNumber(FAKER.finance().creditCard().replace("-", ""))
                .cardType("Visa")
                .expiryMonth(String.format("%02d", FAKER.number().numberBetween(1, 13)))
                .expiryYear(String.valueOf(FAKER.number().numberBetween(2027, 2031)))
                .cvv(String.valueOf(FAKER.number().numberBetween(100, 999)))
                .build();
    }

    public static String generatePhoneNumber() {
        // Demowebshop validates US-style 10-digit numbers — Datafaker's default
        // locale phone strings include separators that the form rejects.
        return String.format("%010d", FAKER.number().numberBetween(2_000_000_000L, 9_999_999_999L));
    }
}
