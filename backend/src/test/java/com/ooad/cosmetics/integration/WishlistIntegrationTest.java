package com.ooad.cosmetics.integration;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.wishlist.WishlistItemResponse;
import com.ooad.cosmetics.service.WishlistService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** W02 - W04: danh sách yêu thích, chạy trên PostgreSQL thật. */
@Tag("integration")
class WishlistIntegrationTest extends IntegrationTestBase {

    @Autowired
    private WishlistService wishlistService;

    private long newProduct(String price) {
        return productIdOf(createVariant(price, 5));
    }

    private void loginAsNewCustomer(String email) {
        createCustomer(email);
        authenticateAs(email, "CUSTOMER");
    }

    @Test
    void addListAndRemoveAProduct() {
        long productId = newProduct("120000.00");
        loginAsNewCustomer("buyer@example.com");

        WishlistItemResponse added = wishlistService.add(productId);
        assertThat(added.productId()).isEqualTo(productId);
        assertThat(added.minPrice()).isEqualByComparingTo(new BigDecimal("120000"));
        assertThat(added.available()).isTrue();

        PageResponse<WishlistItemResponse> page = wishlistService.list(0, 20);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(wishlistService.productIds()).containsExactly(productId);

        wishlistService.remove(productId);
        assertThat(wishlistService.list(0, 20).content()).isEmpty();
        assertThat(count("SELECT COUNT(*) FROM wishlist_items")).isZero();
        // Danh sách (bảng wishlists) vẫn còn để dùng lại.
        assertThat(count("SELECT COUNT(*) FROM wishlists")).isEqualTo(1);
    }

    @Test
    void theSameProductCannotBeAddedTwice() {
        long productId = newProduct("100000.00");
        loginAsNewCustomer("buyer@example.com");
        wishlistService.add(productId);

        assertThatThrownBy(() -> wishlistService.add(productId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product is already in your wishlist");
        assertThat(count("SELECT COUNT(*) FROM wishlist_items")).isEqualTo(1);
    }

    @Test
    void productsThatAreNotOnSaleOrDoNotExistCannotBeAdded() {
        long hidden = newProduct("100000.00");
        long hiddenCategory = newProduct("100000.00");
        jdbc.update("UPDATE products SET status = 'INACTIVE' WHERE id = ?", hidden);
        jdbc.update(
                "UPDATE categories SET status = 'INACTIVE' WHERE id = (SELECT category_id FROM products WHERE id = ?)",
                hiddenCategory);
        loginAsNewCustomer("buyer@example.com");

        assertThatThrownBy(() -> wishlistService.add(hidden))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product is not available");
        assertThatThrownBy(() -> wishlistService.add(hiddenCategory))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> wishlistService.add(999999L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(count("SELECT COUNT(*) FROM wishlist_items")).isZero();
    }

    @Test
    void eachCustomerHasHisOwnWishlist() {
        long productId = newProduct("100000.00");
        loginAsNewCustomer("a@example.com");
        wishlistService.add(productId);

        loginAsNewCustomer("b@example.com");
        assertThat(wishlistService.list(0, 20).content()).isEmpty();
        assertThat(wishlistService.productIds()).isEmpty();
        assertThatThrownBy(() -> wishlistService.remove(productId))
                .isInstanceOf(ResourceNotFoundException.class);

        // Khách B cũng thêm được cùng sản phẩm; danh sách của khách A không bị ảnh hưởng.
        wishlistService.add(productId);
        assertThat(count("SELECT COUNT(*) FROM wishlists")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM wishlist_items")).isEqualTo(2);
    }

    @Test
    void aProductWithoutAnyActiveVariantIsListedAsUnavailable() {
        long variantId = createVariant("100000.00", 5);
        long productId = productIdOf(variantId);
        loginAsNewCustomer("buyer@example.com");
        wishlistService.add(productId);

        jdbc.update("UPDATE product_variants SET status = 'INACTIVE' WHERE id = ?", variantId);

        WishlistItemResponse item = wishlistService.list(0, 20).content().get(0);
        assertThat(item.available()).isFalse();
        assertThat(item.minPrice()).isNull();
    }

    @Test
    void listIsNewestFirstAndPaged() {
        long first = newProduct("100000.00");
        long second = newProduct("100000.00");
        long third = newProduct("100000.00");
        loginAsNewCustomer("buyer@example.com");
        wishlistService.add(first);
        wishlistService.add(second);
        wishlistService.add(third);

        PageResponse<WishlistItemResponse> page = wishlistService.list(0, 2);

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.content()).extracting(WishlistItemResponse::productId)
                .containsExactly(third, second);
        assertThat(wishlistService.productIds()).isEqualTo(List.of(third, second, first));
    }

    @Test
    void deletingAProductAlsoRemovesItFromEveryWishlist() {
        long productId = newProduct("100000.00");
        loginAsNewCustomer("buyer@example.com");
        wishlistService.add(productId);

        jdbc.update("DELETE FROM products WHERE id = ?", productId);

        assertThat(count("SELECT COUNT(*) FROM wishlist_items")).isZero();
        assertThat(wishlistService.list(0, 20).content()).isEmpty();
    }
}
