package com.demowebshop.tests;

import com.demowebshop.base.BaseTest;
import com.demowebshop.dataproviders.ProductDataProvider;
import com.demowebshop.models.BillingAddress;
import com.demowebshop.models.CreditCard;
import com.demowebshop.models.SearchTerm;
import com.demowebshop.models.User;
import com.demowebshop.models.UsersFile;
import com.demowebshop.pages.CartPage;
import com.demowebshop.pages.CheckoutPage;
import com.demowebshop.pages.HeaderComponent;
import com.demowebshop.pages.HomePage;
import com.demowebshop.pages.LoginPage;
import com.demowebshop.pages.OrderConfirmationPage;
import com.demowebshop.pages.SearchResultsPage;
import com.demowebshop.utils.TestDataFactory;
import com.demowebshop.utils.TestDataReader;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Walks the full 11-step purchase flow against demowebshop. Four hard assertions
 * mark the locked checkpoints (login, add-to-cart, cart contents, order confirmation).
 * Driven by {@link ProductDataProvider} — one row today, future terms drop in without
 * changing the test.
 */
@Slf4j
public class EndToEndPurchaseTest extends BaseTest {

    // Shipping/payment labels are demowebshop dropdown values captured during
    // exploration (see AI-docs/locators.md). Short labels match via contains()
    // in the CheckoutPage XPath templates (e.g. "Ground" matches "Ground (0.00)").
    private static final String SHIPPING_METHOD = "Ground";
    private static final String PAYMENT_METHOD  = "Credit Card";

    @Test(dataProvider = "searchTerms",
          dataProviderClass = ProductDataProvider.class,
          description = "End-to-end purchase flow: login, search, add to cart, checkout, confirm, logout")
    public void endToEndPurchaseFlow(SearchTerm searchTerm) {

        log.info("=== Step 1: Launch and verify homepage ===");
        HomePage homePage = new HomePage(getDriver());
        Assert.assertTrue(homePage.isLoaded(),
                "Homepage should be loaded after initial navigation from BaseTest");
        HeaderComponent header = new HeaderComponent(getDriver());

        log.info("=== Step 2: Login ===");
        User user = TestDataReader.load("users.json", UsersFile.class).getDefaultUser();
        LoginPage loginPage = header.clickLoginLink();
        loginPage.login(user);
        Assert.assertTrue(header.isLoggedIn(),
                "User should be logged in after submitting valid credentials");

        log.info("=== Step 3: Search for '{}' ===", searchTerm.getTerm());
        SearchResultsPage resultsPage = header.searchFor(searchTerm.getTerm());
        Assert.assertTrue(resultsPage.isLoaded(),
                "Search results page should load after submitting search");
        Assert.assertTrue(resultsPage.containsProduct(searchTerm.getExpectedProductName()),
                "Search results should contain expected product '" + searchTerm.getExpectedProductName() + "'");

        log.info("=== Step 4: Add '{}' to cart ===", searchTerm.getExpectedProductName());
        resultsPage.addProductToCart(searchTerm.getExpectedProductName());
        Assert.assertTrue(header.getCartItemCount() > 0,
                "Cart item count should be greater than 0 after adding a product");

        log.info("=== Step 5: Open cart and verify contents ===");
        CartPage cartPage = header.openCart();
        Assert.assertTrue(cartPage.isLoaded(), "Cart page should load after clicking cart link");
        List<String> cartItemNames = cartPage.getCartItemNames();
        Assert.assertTrue(cartItemNames.contains(searchTerm.getExpectedProductName()),
                "Cart should contain '" + searchTerm.getExpectedProductName()
                        + "' but had: " + cartItemNames);

        log.info("=== Step 6: Proceed to checkout ===");
        CheckoutPage checkoutPage = cartPage.checkout();
        Assert.assertTrue(checkoutPage.isLoaded(),
                "Checkout page should load after accepting terms and clicking Checkout");

        log.info("=== Step 7: Fill billing and shipping addresses ===");
        BillingAddress address = TestDataFactory.generateBillingAddress();
        checkoutPage.fillBillingAddress(address);
        checkoutPage.fillShippingAddress(address);

        log.info("=== Step 8: Select shipping method '{}' and payment method '{}' ===",
                SHIPPING_METHOD, PAYMENT_METHOD);
        checkoutPage.selectShippingMethod(SHIPPING_METHOD);
        checkoutPage.selectPaymentMethod(PAYMENT_METHOD);

        log.info("=== Step 9: Fill payment info and confirm order ===");
        CreditCard card = TestDataFactory.generateCreditCard();
        checkoutPage.fillPaymentInfo(card);
        OrderConfirmationPage confirmation = checkoutPage.confirmOrder();

        log.info("=== Step 10: Verify order success ===");
        Assert.assertTrue(confirmation.isLoaded(),
                "Order confirmation page should be loaded after clicking Confirm");
        Assert.assertTrue(confirmation.isOrderPlaced(),
                "Success banner should indicate the order was processed successfully");
        String orderNumber = confirmation.getOrderNumber();
        Assert.assertFalse(orderNumber.isEmpty(),
                "Order number should be captured from the confirmation page");
        log.info("Order placed successfully. Order number: {}", orderNumber);

        log.info("=== Step 11: Logout ===");
        HeaderComponent postOrderHeader = new HeaderComponent(getDriver());
        postOrderHeader.clickLogout();
        Assert.assertFalse(new HeaderComponent(getDriver()).isLoggedIn(),
                "User should be logged out after clicking the logout link");
    }
}
