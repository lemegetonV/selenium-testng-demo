package com.demowebshop.utils;

import com.demowebshop.config.ConfigReader;
import com.demowebshop.core.FrameworkException;
import com.demowebshop.models.BillingAddress;
import com.demowebshop.models.CreditCard;
import com.demowebshop.models.TestCard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
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
    private static final List<TestCard> TEST_CARDS;

    static {
        long seed = ConfigReader.getInt("dataFakerSeed", 42);
        FAKER = new Faker(Locale.US, new Random(seed));
        log.info("TestDataFactory seeded with dataFakerSeed={}", seed);

        TEST_CARDS = loadTestCards();
    }

    private TestDataFactory() {
        // static utility — no instances
    }

    private static List<TestCard> loadTestCards() {
        String resource = "testdata/paymentCards.json";
        try (InputStream stream = TestDataFactory.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new FrameworkException("paymentCards.json not found on classpath: " + resource);
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(stream);
            JsonNode cards = root.get("testCards");
            return mapper.convertValue(cards, new TypeReference<List<TestCard>>() {});
        } catch (FrameworkException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new FrameworkException("Failed to load paymentCards.json", ex);
        }
    }

    public static BillingAddress generateBillingAddress() {
        return BillingAddress.builder()
                .firstName(FAKER.name().firstName())
                .lastName(FAKER.name().lastName())
                .email(FAKER.internet().emailAddress())
                .country("United States")
                // Country hardcoded — state dropdown options are country-dependent;
                // faker-generated country would break the state selection.
                .state(FAKER.address().state())
                .city(FAKER.address().city())
                .address1(FAKER.address().streetAddress())
                .zip(FAKER.address().zipCode())
                .phone(generatePhoneNumber())
                .build();
    }

    /**
     * Builds a {@link CreditCard} using a Luhn-valid card number from
     * {@code paymentCards.json}. The card number is NOT generated randomly —
     * demowebshop runs a Luhn checksum on submission and random digits will
     * fail. The first known-good entry is always selected for determinism.
     * Dynamic fields (cardholder, expiry, CVV) are Datafaker-generated.
     */
    public static CreditCard generateCreditCard() {
        TestCard testCard = TEST_CARDS.get(0);

        return CreditCard.builder()
                .cardType(testCard.getCardType())
                .cardNumber(testCard.getCardNumber())
                .cardholder(FAKER.name().fullName())
                .expiryMonth("12")
                // Expiry year is dynamic so it stays valid as calendar years advance.
                .expiryYear(String.valueOf(LocalDate.now().getYear() + 4))
                .cvv(FAKER.numerify("###"))
                .build();
    }

    public static String generatePhoneNumber() {
        // Demowebshop validates US-style 10-digit numbers — Datafaker's default
        // locale phone strings include separators that the form rejects.
        return String.format("%010d", FAKER.number().numberBetween(2_000_000_000L, 9_999_999_999L));
    }
}
