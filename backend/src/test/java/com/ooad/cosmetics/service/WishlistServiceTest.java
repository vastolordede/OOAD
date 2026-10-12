package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.wishlist.WishlistItemResponse;
import com.ooad.cosmetics.entity.Brand;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.Category;
import com.ooad.cosmetics.entity.Product;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.entity.Wishlist;
import com.ooad.cosmetics.entity.WishlistItem;
import com.ooad.cosmetics.repository.ProductImageRepository;
import com.ooad.cosmetics.repository.ProductRepository;
import com.ooad.cosmetics.repository.ProductVariantRepository;
import com.ooad.cosmetics.repository.WishlistItemRepository;
import com.ooad.cosmetics.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WishlistServiceTest {

    private WishlistRepository wishlistRepository;
    private WishlistItemRepository itemRepository;
    private ProductRepository productRepository;
    private ProductVariantRepository variantRepository;
    private WishlistService service;
    private User user;
    private Wishlist wishlist;

    @BeforeEach
    void setUp() {
        wishlistRepository = mock(WishlistRepository.class);
        itemRepository = mock(WishlistItemRepository.class);
        productRepository = mock(ProductRepository.class);
        variantRepository = mock(ProductVariantRepository.class);
        ProductImageRepository imageRepository = mock(ProductImageRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);

        service = new WishlistService(
                wishlistRepository,
                itemRepository,
                productRepository,
                variantRepository,
                imageRepository,
                currentUserService
        );

        user = new User("customer@example.com", "hash", "Customer", null);
        ReflectionTestUtils.setField(user, "id", 1L);

        wishlist = new Wishlist();
        ReflectionTestUtils.setField(wishlist, "id", 50L);
        wishlist.setUser(user);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(wishlist));
        when(itemRepository.save(any(WishlistItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(variantRepository.findMinActivePrice(5L)).thenReturn(new BigDecimal("100000.00"));
    }

    // ---------------- add ----------------

    @Test
    void addSavesTheProductIntoTheWishlist() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5L, CatalogStatus.ACTIVE)));
        when(itemRepository.existsByWishlistIdAndProductId(50L, 5L)).thenReturn(false);

        WishlistItemResponse response = service.add(5L);

        verify(itemRepository).save(any(WishlistItem.class));
        assertThat(response.productId()).isEqualTo(5L);
        assertThat(response.productName()).isEqualTo("Serum");
        assertThat(response.brandName()).isEqualTo("Brand");
        assertThat(response.minPrice()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(response.available()).isTrue();
    }

    @Test
    void addCreatesTheWishlistOnFirstUse() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5L, CatalogStatus.ACTIVE)));

        service.add(5L);

        verify(wishlistRepository).save(any(Wishlist.class));
        verify(itemRepository).save(any(WishlistItem.class));
    }

    @Test
    void addRejectsAnUnknownProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addRejectsAProductThatIsNotOnSale() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5L, CatalogStatus.INACTIVE)));

        assertThatThrownBy(() -> service.add(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product is not available");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void addRejectsAProductWhoseCategoryIsInactive() {
        Product product = product(5L, CatalogStatus.ACTIVE);
        product.getCategory().setStatus(CatalogStatus.INACTIVE);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.add(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product is not available");
    }

    @Test
    void addRejectsAProductAlreadyInTheWishlist() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5L, CatalogStatus.ACTIVE)));
        when(itemRepository.existsByWishlistIdAndProductId(50L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.add(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product is already in your wishlist");

        verify(itemRepository, never()).save(any());
    }

    // ---------------- remove ----------------

    @Test
    void removeDeletesTheItem() {
        WishlistItem item = item(product(5L, CatalogStatus.ACTIVE));
        when(itemRepository.findByWishlistIdAndProductId(50L, 5L)).thenReturn(Optional.of(item));

        service.remove(5L);

        verify(itemRepository).delete(item);
    }

    @Test
    void removeRejectsAProductThatIsNotInTheWishlist() {
        when(itemRepository.findByWishlistIdAndProductId(50L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeRejectsWhenTheCustomerHasNoWishlistYet() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(itemRepository, never()).delete(any(WishlistItem.class));
    }

    // ---------------- list ----------------

    @Test
    void listReturnsAnEmptyPageWhenTheCustomerHasNoWishlist() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.empty());

        PageResponse<WishlistItemResponse> page = service.list(0, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void listMarksAProductUnavailableWhenNoVariantIsOnSaleAnymore() {
        Product product = product(5L, CatalogStatus.ACTIVE);
        when(variantRepository.findMinActivePrice(5L)).thenReturn(null);
        when(itemRepository.findByWishlistId(eq(50L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item(product))));

        PageResponse<WishlistItemResponse> page = service.list(0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).available()).isFalse();
        assertThat(page.content().get(0).minPrice()).isNull();
    }

    @Test
    void productIdsReturnsTheIdsFromTheRepository() {
        when(itemRepository.findProductIdsByWishlistId(50L)).thenReturn(List.of(7L, 5L));

        assertThat(service.productIds()).containsExactly(7L, 5L);
    }

    @Test
    void productIdsIsEmptyWhenTheCustomerHasNoWishlist() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThat(service.productIds()).isEmpty();
    }

    // ---------------- helpers ----------------

    private Product product(Long id, CatalogStatus status) {
        Category category = new Category();
        category.setName("Skincare");
        Brand brand = new Brand();
        brand.setName("Brand");

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setName("Serum");
        product.setCategory(category);
        product.setBrand(brand);
        product.setStatus(status);
        return product;
    }

    private WishlistItem item(Product product) {
        WishlistItem item = new WishlistItem();
        item.setWishlist(wishlist);
        item.setProduct(product);
        return item;
    }
}
