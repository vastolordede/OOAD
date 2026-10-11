package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.review.AdminReviewResponse;
import com.ooad.cosmetics.dto.review.CreateReviewRequest;
import com.ooad.cosmetics.dto.review.ProductReviewsResponse;
import com.ooad.cosmetics.dto.review.ReviewResponse;
import com.ooad.cosmetics.dto.review.UpdateReviewRequest;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.OrderItem;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.Product;
import com.ooad.cosmetics.entity.Review;
import com.ooad.cosmetics.entity.ReviewStatus;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.repository.OrderItemRepository;
import com.ooad.cosmetics.repository.ProductRepository;
import com.ooad.cosmetics.repository.ReviewRepository;
import com.ooad.cosmetics.repository.ReviewSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    public ReviewService(
            ReviewRepository reviewRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            CurrentUserService currentUserService
    ) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
    }

    // ------------------------------------------------------------------
    // Customer
    // ------------------------------------------------------------------

    /** R02 - R05 */
    @Transactional
    public ReviewResponse create(CreateReviewRequest request) {
        validateRating(request.rating());
        User user = currentUserService.requireCurrentUser();

        // Dòng hàng phải thuộc đơn của chính khách này.
        OrderItem item = orderItemRepository
                .findByIdAndOrderUserId(request.orderItemId(), user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("OrderItem", request.orderItemId())
                );

        // R03
        if (item.getOrder().getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("You can only review products from delivered orders");
        }
        // R05
        if (reviewRepository.existsByOrderItemId(item.getId())) {
            throw new BadRequestException("This item has already been reviewed");
        }

        Review review = new Review();
        review.setOrderItem(item);
        review.setUser(user);
        review.setProduct(item.getVariant().getProduct());
        review.setRating(request.rating());
        review.setComment(normalizeNullable(request.comment()));

        return ReviewResponse.from(reviewRepository.save(review));
    }

    /** R16 */
    @Transactional
    public ReviewResponse update(Long id, UpdateReviewRequest request) {
        validateRating(request.rating());
        User user = currentUserService.requireCurrentUser();
        Review review = requireOwnedEditable(id, user.getId());

        review.setRating(request.rating());
        review.setComment(normalizeNullable(request.comment()));
        return ReviewResponse.from(review);
    }

    /** R16 */
    @Transactional
    public void delete(Long id) {
        User user = currentUserService.requireCurrentUser();
        Review review = requireOwnedEditable(id, user.getId());
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> myReviews(int page, int size) {
        User user = currentUserService.requireCurrentUser();
        return PageResponse.from(
                reviewRepository.findByUserId(user.getId(), newestFirst(page, size))
                        .map(ReviewResponse::from)
        );
    }

    // ------------------------------------------------------------------
    // Public
    // ------------------------------------------------------------------

    /** R06, R07, R08: chỉ tính các review đang VISIBLE. */
    @Transactional(readOnly = true)
    public ProductReviewsResponse productReviews(Long productId, int page, int size) {
        requirePublicProduct(productId);

        long count = reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.VISIBLE);
        double average = 0.0;
        if (count > 0) {
            Double raw = reviewRepository.averageRating(productId, ReviewStatus.VISIBLE);
            average = raw == null ? 0.0 : Math.round(raw * 10.0) / 10.0;
        }

        Page<Review> reviews = reviewRepository.findByProductIdAndStatus(
                productId,
                ReviewStatus.VISIBLE,
                newestFirst(page, size)
        );

        return new ProductReviewsResponse(
                average,
                count,
                PageResponse.from(reviews.map(ReviewResponse::from))
        );
    }

    // ------------------------------------------------------------------
    // Admin
    // ------------------------------------------------------------------

    /** R13 */
    @Transactional(readOnly = true)
    public PageResponse<AdminReviewResponse> adminList(
            ReviewStatus status,
            Long productId,
            int page,
            int size
    ) {
        return PageResponse.from(
                reviewRepository
                        .findAll(ReviewSpecifications.adminFilter(status, productId),
                                newestFirst(page, size))
                        .map(AdminReviewResponse::from)
        );
    }

    /** R09, R10: ẩn / hiện review. */
    @Transactional
    public AdminReviewResponse setStatus(Long id, ReviewStatus status) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", id));
        review.setStatus(status);
        return AdminReviewResponse.from(review);
    }

    // ------------------------------------------------------------------

    private Review requireOwnedEditable(Long id, Long userId) {
        Review review = reviewRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", id));

        // Không cho sửa/xóa review đã bị ẩn, tránh xóa đi viết lại để lách kiểm duyệt.
        if (review.getStatus() == ReviewStatus.HIDDEN) {
            throw new BadRequestException("This review has been hidden by a moderator");
        }
        return review;
    }

    private void requirePublicProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (product.getStatus() != CatalogStatus.ACTIVE
                || product.getCategory().getStatus() != CatalogStatus.ACTIVE
                || product.getBrand().getStatus() != CatalogStatus.ACTIVE) {
            throw new ResourceNotFoundException("Product", productId);
        }
    }

    /** R04 */
    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("rating must be between 1 and 5");
        }
    }

    private PageRequest newestFirst(int page, int size) {
        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
