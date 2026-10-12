package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.wishlist.WishlistItemResponse;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.Product;
import com.ooad.cosmetics.entity.ProductImage;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.entity.Wishlist;
import com.ooad.cosmetics.entity.WishlistItem;
import com.ooad.cosmetics.repository.ProductImageRepository;
import com.ooad.cosmetics.repository.ProductRepository;
import com.ooad.cosmetics.repository.ProductVariantRepository;
import com.ooad.cosmetics.repository.WishlistItemRepository;
import com.ooad.cosmetics.repository.WishlistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CurrentUserService currentUserService;

    public WishlistService(
            WishlistRepository wishlistRepository,
            WishlistItemRepository itemRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            ProductImageRepository imageRepository,
            CurrentUserService currentUserService
    ) {
        this.wishlistRepository = wishlistRepository;
        this.itemRepository = itemRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.currentUserService = currentUserService;
    }

    /** W04: danh sách yêu thích của chính khách, mới thêm đứng đầu. */
    @Transactional(readOnly = true)
    public PageResponse<WishlistItemResponse> list(int page, int size) {
        User user = currentUserService.requireCurrentUser();
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        return wishlistRepository.findByUserId(user.getId())
                .map(wishlist -> {
                    Page<WishlistItem> items = itemRepository.findByWishlistId(wishlist.getId(), pageable);
                    return PageResponse.from(items.map(this::toResponse));
                })
                .orElseGet(() -> PageResponse.from(Page.<WishlistItemResponse>empty(pageable)));
    }

    /** Id các sản phẩm đang yêu thích, để giao diện tô sáng icon trái tim. */
    @Transactional(readOnly = true)
    public List<Long> productIds() {
        User user = currentUserService.requireCurrentUser();
        return wishlistRepository.findByUserId(user.getId())
                .map(wishlist -> itemRepository.findProductIdsByWishlistId(wishlist.getId()))
                .orElseGet(List::of);
    }

    /** W02 */
    @Transactional
    public WishlistItemResponse add(Long productId) {
        User user = currentUserService.requireCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (!isPubliclyVisible(product)) {
            throw new BadRequestException("Product is not available");
        }

        Wishlist wishlist = wishlistRepository.findByUserId(user.getId())
                .orElseGet(() -> createWishlist(user));

        if (itemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            throw new BadRequestException("Product is already in your wishlist");
        }

        WishlistItem item = new WishlistItem();
        item.setWishlist(wishlist);
        item.setProduct(product);
        return toResponse(itemRepository.save(item));
    }

    /** W03 */
    @Transactional
    public void remove(Long productId) {
        User user = currentUserService.requireCurrentUser();

        Wishlist wishlist = wishlistRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("WishlistItem", productId));
        WishlistItem item = itemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("WishlistItem", productId));

        itemRepository.delete(item);
    }

    // ------------------------------------------------------------------

    private Wishlist createWishlist(User user) {
        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        return wishlistRepository.save(wishlist);
    }

    private boolean isPubliclyVisible(Product product) {
        return product.getStatus() == CatalogStatus.ACTIVE
                && product.getCategory().getStatus() == CatalogStatus.ACTIVE
                && product.getBrand().getStatus() == CatalogStatus.ACTIVE;
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        Product product = item.getProduct();

        // Không còn variant nào đang bán thì sản phẩm hiện là "hết bán", nhưng vẫn nằm trong danh sách.
        BigDecimal minPrice = variantRepository.findMinActivePrice(product.getId());

        List<ProductImage> images = imageRepository
                .findByProductIdOrderByPrimaryImageDescSortOrderAscIdAsc(product.getId());
        String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

        return new WishlistItemResponse(
                product.getId(),
                product.getName(),
                product.getBrand().getName(),
                minPrice,
                imageUrl,
                isPubliclyVisible(product) && minPrice != null,
                item.getCreatedAt()
        );
    }
}
