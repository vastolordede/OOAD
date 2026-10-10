package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.dto.cart.AddCartItemRequest;
import com.ooad.cosmetics.dto.cart.CartResponse;
import com.ooad.cosmetics.dto.cart.UpdateCartItemRequest;
import com.ooad.cosmetics.entity.Brand;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.Cart;
import com.ooad.cosmetics.entity.CartItem;
import com.ooad.cosmetics.entity.CartStatus;
import com.ooad.cosmetics.entity.Category;
import com.ooad.cosmetics.entity.Product;
import com.ooad.cosmetics.entity.ProductVariant;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.repository.CartItemRepository;
import com.ooad.cosmetics.repository.CartRepository;
import com.ooad.cosmetics.repository.ProductImageRepository;
import com.ooad.cosmetics.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {

    private CartItemRepository cartItemRepository;
    private ProductVariantRepository variantRepository;
    private CartService service;
    private Cart cart;

    @BeforeEach
    void setUp() {
        CartRepository cartRepository = mock(CartRepository.class);
        cartItemRepository = mock(CartItemRepository.class);
        variantRepository = mock(ProductVariantRepository.class);
        ProductImageRepository imageRepository = mock(ProductImageRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);

        service = new CartService(
                cartRepository,
                cartItemRepository,
                variantRepository,
                imageRepository,
                currentUserService
        );

        User user = new User("customer@example.com", "hash", "Customer", null);
        ReflectionTestUtils.setField(user, "id", 1L);

        cart = new Cart();
        ReflectionTestUtils.setField(cart, "id", 100L);
        cart.setUser(user);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
    }

    @Test
    void addItemRejectsInactiveVariant() {
        ProductVariant variant = variant(10L, "100000", 5, CatalogStatus.INACTIVE);
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> service.addItem(new AddCartItemRequest(10L, 1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product is not available");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItemRejectsUnknownVariant() {
        when(variantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItem(new AddCartItemRequest(99L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addItemRejectsQuantityOverStockIncludingWhatIsAlreadyInCart() {
        ProductVariant variant = variant(10L, "100000", 5, CatalogStatus.ACTIVE);
        CartItem existing = item(variant, 3);

        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndVariantId(100L, 10L))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.addItem(new AddCartItemRequest(10L, 3)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Quantity exceeds available stock (5)");

        assertThat(existing.getQuantity()).isEqualTo(3);
    }

    @Test
    void addItemMergesQuantityWhenVariantAlreadyInCart() {
        ProductVariant variant = variant(10L, "100000", 10, CatalogStatus.ACTIVE);
        CartItem existing = item(variant, 2);

        when(variantRepository.findById(10L)).thenReturn(Optional.of(variant));
        when(cartItemRepository.findByCartIdAndVariantId(100L, 10L))
                .thenReturn(Optional.of(existing));
        when(cartItemRepository.findByCartIdOrderByIdAsc(100L))
                .thenReturn(List.of(existing));

        CartResponse response = service.addItem(new AddCartItemRequest(10L, 3));

        assertThat(existing.getQuantity()).isEqualTo(5);
        assertThat(response.totalQuantity()).isEqualTo(5);
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    void updateItemRejectsItemThatIsNotInMyCart() {
        when(cartItemRepository.findByIdAndCartId(7L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateItem(7L, new UpdateCartItemRequest(2)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItemRejectsQuantityOverStock() {
        ProductVariant variant = variant(10L, "100000", 4, CatalogStatus.ACTIVE);
        CartItem existing = item(variant, 1);
        when(cartItemRepository.findByIdAndCartId(7L, 100L))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateItem(7L, new UpdateCartItemRequest(5)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Quantity exceeds available stock (4)");
    }

    @Test
    void getMyCartSubtotalSkipsUnavailableItems() {
        CartItem available = item(variant(10L, "100000", 5, CatalogStatus.ACTIVE), 2);
        CartItem unavailable = item(variant(11L, "50000", 5, CatalogStatus.INACTIVE), 1);

        when(cartItemRepository.findByCartIdOrderByIdAsc(100L))
                .thenReturn(List.of(available, unavailable));

        CartResponse response = service.getMyCart();

        assertThat(response.items()).hasSize(2);
        assertThat(response.totalQuantity()).isEqualTo(3);
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(response.hasUnavailableItems()).isTrue();
    }

    @Test
    void getMyCartMarksItemUnavailableWhenStockDroppedBelowQuantity() {
        CartItem item = item(variant(10L, "100000", 1, CatalogStatus.ACTIVE), 3);
        when(cartItemRepository.findByCartIdOrderByIdAsc(100L))
                .thenReturn(List.of(item));

        CartResponse response = service.getMyCart();

        assertThat(response.items().get(0).available()).isFalse();
        assertThat(response.subtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private CartItem item(ProductVariant variant, int quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setVariant(variant);
        item.setQuantity(quantity);
        return item;
    }

    private ProductVariant variant(Long id, String price, int stock, CatalogStatus status) {
        Category category = new Category();
        category.setName("Skincare");
        Brand brand = new Brand();
        brand.setName("Brand");

        Product product = new Product();
        product.setName("Product " + id);
        product.setCategory(category);
        product.setBrand(brand);

        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", id);
        variant.setProduct(product);
        variant.setSku("SKU-" + id);
        variant.setPrice(new BigDecimal(price));
        variant.setStock(stock);
        variant.setStatus(status);
        return variant;
    }
}
