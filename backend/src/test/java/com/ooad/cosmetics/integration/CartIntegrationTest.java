package com.ooad.cosmetics.integration;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.dto.cart.AddCartItemRequest;
import com.ooad.cosmetics.dto.cart.CartResponse;
import com.ooad.cosmetics.dto.cart.UpdateCartItemRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** C17: kiểm tra Cart API và kiểm tra tồn kho, trên PostgreSQL thật. */
@Tag("integration")
class CartIntegrationTest extends IntegrationTestBase {

    @Test
    void firstCallCreatesExactlyOneActiveCartAndLaterCallsReuseIt() {
        createCustomer("buyer@example.com");
        authenticateAs("buyer@example.com", "CUSTOMER");

        CartResponse first = cartService.getMyCart();
        CartResponse second = cartService.getMyCart();

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(count("SELECT COUNT(*) FROM carts WHERE status = 'ACTIVE'")).isEqualTo(1);
    }

    @Test
    void addingTheSameVariantTwiceMergesQuantityIntoOneRow() {
        long variantId = createVariant("100000.00", 10);
        prepareCustomerWithCart("buyer@example.com", variantId, 2);

        CartResponse cart = cartService.addItem(new AddCartItemRequest(variantId, 3));

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).quantity()).isEqualTo(5);
        assertThat(cart.subtotal()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(count("SELECT COUNT(*) FROM cart_items")).isEqualTo(1);
    }

    @Test
    void quantityBeyondStockIsRejectedAndNothingIsChanged() {
        long variantId = createVariant("100000.00", 5);
        prepareCustomerWithCart("buyer@example.com", variantId, 3);

        assertThatThrownBy(() -> cartService.addItem(new AddCartItemRequest(variantId, 3)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Quantity exceeds available stock (5)");

        assertThat(jdbc.queryForObject("SELECT quantity FROM cart_items", Integer.class)).isEqualTo(3);
    }

    @Test
    void inactiveVariantCannotBeAddedToCart() {
        long variantId = createVariant("100000.00", 5);
        jdbc.update("UPDATE product_variants SET status = 'INACTIVE' WHERE id = ?", variantId);
        createCustomer("buyer@example.com");
        authenticateAs("buyer@example.com", "CUSTOMER");

        assertThatThrownBy(() -> cartService.addItem(new AddCartItemRequest(variantId, 1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product is not available");
    }

    @Test
    void updateAndRemoveAndClearWorkOnlyOnMyOwnCart() {
        long variantId = createVariant("100000.00", 10);
        prepareCustomerWithCart("buyer@example.com", variantId, 2);
        Long itemId = jdbc.queryForObject("SELECT id FROM cart_items", Long.class);

        CartResponse updated = cartService.updateItem(itemId, new UpdateCartItemRequest(4));
        assertThat(updated.totalQuantity()).isEqualTo(4);

        // Khách khác không được đụng vào dòng hàng này.
        createCustomer("other@example.com");
        authenticateAs("other@example.com", "CUSTOMER");
        assertThatThrownBy(() -> cartService.removeItem(itemId))
                .isInstanceOf(com.ooad.cosmetics.common.exception.ResourceNotFoundException.class);
        assertThat(count("SELECT COUNT(*) FROM cart_items")).isEqualTo(1);

        authenticateAs("buyer@example.com", "CUSTOMER");
        CartResponse cleared = cartService.clear();
        assertThat(cleared.items()).isEmpty();
        assertThat(count("SELECT COUNT(*) FROM cart_items")).isZero();
    }

    @Test
    void cartMarksItemUnavailableWhenStockDropsBelowQuantityAfterwards() {
        long variantId = createVariant("100000.00", 10);
        prepareCustomerWithCart("buyer@example.com", variantId, 5);

        jdbc.update("UPDATE product_variants SET stock = 2 WHERE id = ?", variantId);

        CartResponse cart = cartService.getMyCart();
        assertThat(cart.hasUnavailableItems()).isTrue();
        assertThat(cart.items().get(0).available()).isFalse();
        assertThat(cart.subtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
