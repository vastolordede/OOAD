package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.dto.cart.AddCartItemRequest;
import com.ooad.cosmetics.dto.cart.CartItemResponse;
import com.ooad.cosmetics.dto.cart.CartResponse;
import com.ooad.cosmetics.dto.cart.UpdateCartItemRequest;
import com.ooad.cosmetics.entity.Cart;
import com.ooad.cosmetics.entity.CartItem;
import com.ooad.cosmetics.entity.CartStatus;
import com.ooad.cosmetics.entity.Product;
import com.ooad.cosmetics.entity.ProductImage;
import com.ooad.cosmetics.entity.ProductVariant;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.repository.CartItemRepository;
import com.ooad.cosmetics.repository.CartRepository;
import com.ooad.cosmetics.repository.ProductImageRepository;
import com.ooad.cosmetics.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CurrentUserService currentUserService;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductVariantRepository variantRepository,
            ProductImageRepository imageRepository,
            CurrentUserService currentUserService
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.currentUserService = currentUserService;
    }

    /** C05: lấy giỏ hiện tại (tự tạo nếu chưa có, C03). */
    @Transactional
    public CartResponse getMyCart() {
        return toResponse(getOrCreateActiveCart());
    }

    /** C06: thêm sản phẩm; nếu variant đã có trong giỏ thì cộng dồn số lượng. */
    @Transactional
    public CartResponse addItem(AddCartItemRequest request) {
        Cart cart = getOrCreateActiveCart();

        ProductVariant variant = variantRepository.findById(request.variantId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("ProductVariant", request.variantId())
                );
        requireSellable(variant);

        CartItem item = cartItemRepository
                .findByCartIdAndVariantId(cart.getId(), variant.getId())
                .orElse(null);

        int newQuantity = request.quantity() + (item == null ? 0 : item.getQuantity());
        requireEnoughStock(variant, newQuantity);

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setVariant(variant);
        }
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        return toResponse(cart);
    }

    /** C07: đặt lại số lượng của một dòng trong giỏ. */
    @Transactional
    public CartResponse updateItem(Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateActiveCart();
        CartItem item = requireItem(itemId, cart.getId());

        requireSellable(item.getVariant());
        requireEnoughStock(item.getVariant(), request.quantity());

        item.setQuantity(request.quantity());
        return toResponse(cart);
    }

    /** C08 */
    @Transactional
    public CartResponse removeItem(Long itemId) {
        Cart cart = getOrCreateActiveCart();
        CartItem item = requireItem(itemId, cart.getId());

        cartItemRepository.delete(item);
        cartItemRepository.flush();

        return toResponse(cart);
    }

    /** C09 */
    @Transactional
    public CartResponse clear() {
        Cart cart = getOrCreateActiveCart();
        cartItemRepository.deleteAllByCartId(cart.getId());
        return toResponse(cart);
    }

    private Cart getOrCreateActiveCart() {
        User user = currentUserService.requireCurrentUser();

        return cartRepository
                .findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    private CartItem requireItem(Long itemId, Long cartId) {
        return cartItemRepository.findByIdAndCartId(itemId, cartId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));
    }

    /** C10 */
    private void requireSellable(ProductVariant variant) {
        if (!VariantAvailability.isSellable(variant)) {
            throw new BadRequestException("Product is not available");
        }
    }

    /** C11, C12 (quantity > 0 đã được @Min(1) ở DTO chặn). */
    private void requireEnoughStock(ProductVariant variant, int quantity) {
        if (quantity > variant.getStock()) {
            throw new BadRequestException(
                    "Quantity exceeds available stock (" + variant.getStock() + ")"
            );
        }
    }

    /** C13: backend tự tính subtotal; chỉ cộng các dòng còn mua được. */
    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepository
                .findByCartIdOrderByIdAsc(cart.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();

        int totalQuantity = items.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        BigDecimal subtotal = items.stream()
                .filter(CartItemResponse::available)
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasUnavailable = items.stream().anyMatch(item -> !item.available());

        return new CartResponse(
                cart.getId(),
                items,
                totalQuantity,
                subtotal,
                hasUnavailable
        );
    }

    private CartItemResponse toItemResponse(CartItem item) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();

        BigDecimal lineTotal = variant.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        boolean available = VariantAvailability.isSellable(variant)
                && variant.getStock() >= item.getQuantity();

        List<ProductImage> images = imageRepository
                .findByProductIdOrderByPrimaryImageDescSortOrderAscIdAsc(product.getId());
        String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

        return new CartItemResponse(
                item.getId(),
                variant.getId(),
                product.getId(),
                product.getName(),
                variant.getSku(),
                variant.getPrice(),
                item.getQuantity(),
                lineTotal,
                variant.getStock(),
                available,
                imageUrl
        );
    }
}
