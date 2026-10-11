package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.dto.review.AdminReviewResponse;
import com.ooad.cosmetics.dto.review.CreateReviewRequest;
import com.ooad.cosmetics.dto.review.ProductReviewsResponse;
import com.ooad.cosmetics.dto.review.ReviewResponse;
import com.ooad.cosmetics.dto.review.UpdateReviewRequest;
import com.ooad.cosmetics.entity.Brand;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.Category;
import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderItem;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.Product;
import com.ooad.cosmetics.entity.ProductVariant;
import com.ooad.cosmetics.entity.Review;
import com.ooad.cosmetics.entity.ReviewStatus;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.repository.OrderItemRepository;
import com.ooad.cosmetics.repository.ProductRepository;
import com.ooad.cosmetics.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

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

class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private OrderItemRepository orderItemRepository;
    private ProductRepository productRepository;
    private ReviewService service;
    private User user;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        productRepository = mock(ProductRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);

        service = new ReviewService(
                reviewRepository,
                orderItemRepository,
                productRepository,
                currentUserService
        );

        user = new User("customer@example.com", "hash", "Nguyen Van An", null);
        ReflectionTestUtils.setField(user, "id", 1L);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------- create ----------------

    @Test
    void createSavesReviewForDeliveredOrderOfTheCustomer() {
        OrderItem item = orderItem(30L, OrderStatus.DELIVERED);
        when(orderItemRepository.findByIdAndOrderUserId(30L, 1L)).thenReturn(Optional.of(item));
        when(reviewRepository.existsByOrderItemId(30L)).thenReturn(false);

        ReviewResponse response = service.create(new CreateReviewRequest(30L, 5, "  Rat tot  "));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getComment()).isEqualTo("Rat tot");
        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.VISIBLE);
        assertThat(saved.getProduct().getId()).isEqualTo(5L);
        assertThat(saved.getUser()).isSameAs(user);

        assertThat(response.productId()).isEqualTo(5L);
        assertThat(response.orderItemId()).isEqualTo(30L);
        assertThat(response.reviewerName()).isEqualTo("Nguyen A.");
    }

    @Test
    void createRejectsOrderItemOfAnotherCustomer() {
        when(orderItemRepository.findByIdAndOrderUserId(30L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateReviewRequest(30L, 4, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reviewRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderStatus.class,
            names = {"PENDING", "CONFIRMED", "SHIPPING", "CANCELLED"}
    )
    void createRejectsOrdersThatAreNotDelivered(OrderStatus status) {
        when(orderItemRepository.findByIdAndOrderUserId(30L, 1L))
                .thenReturn(Optional.of(orderItem(30L, status)));

        assertThatThrownBy(() -> service.create(new CreateReviewRequest(30L, 4, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You can only review products from delivered orders");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createRejectsAnItemThatWasAlreadyReviewed() {
        when(orderItemRepository.findByIdAndOrderUserId(30L, 1L))
                .thenReturn(Optional.of(orderItem(30L, OrderStatus.DELIVERED)));
        when(reviewRepository.existsByOrderItemId(30L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateReviewRequest(30L, 4, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This item has already been reviewed");

        verify(reviewRepository, never()).save(any());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 6, -1})
    void createRejectsRatingOutsideOneToFive(int rating) {
        assertThatThrownBy(() -> service.create(new CreateReviewRequest(30L, rating, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("rating must be between 1 and 5");
    }

    // ---------------- update / delete ----------------

    @Test
    void updateChangesRatingAndComment() {
        Review review = review(40L, 3, ReviewStatus.VISIBLE);
        when(reviewRepository.findByIdAndUserId(40L, 1L)).thenReturn(Optional.of(review));

        ReviewResponse response = service.update(40L, new UpdateReviewRequest(5, " Doi y, rat tot "));

        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("Doi y, rat tot");
        assertThat(response.rating()).isEqualTo(5);
    }

    @Test
    void updateRejectsReviewOfAnotherCustomer() {
        when(reviewRepository.findByIdAndUserId(40L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(40L, new UpdateReviewRequest(5, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRejectsAReviewThatWasHiddenByAModerator() {
        Review review = review(40L, 3, ReviewStatus.HIDDEN);
        when(reviewRepository.findByIdAndUserId(40L, 1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.update(40L, new UpdateReviewRequest(5, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This review has been hidden by a moderator");

        assertThat(review.getRating()).isEqualTo(3);
    }

    @Test
    void deleteRemovesOwnVisibleReview() {
        Review review = review(40L, 3, ReviewStatus.VISIBLE);
        when(reviewRepository.findByIdAndUserId(40L, 1L)).thenReturn(Optional.of(review));

        service.delete(40L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteRejectsAHiddenReviewSoModerationCannotBeBypassed() {
        Review review = review(40L, 3, ReviewStatus.HIDDEN);
        when(reviewRepository.findByIdAndUserId(40L, 1L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.delete(40L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This review has been hidden by a moderator");

        verify(reviewRepository, never()).delete(any(Review.class));
    }

    // ---------------- public list / rating ----------------

    @Test
    void productReviewsComputesRoundedAverageAndCountFromVisibleReviews() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5L, CatalogStatus.ACTIVE)));
        when(reviewRepository.countByProductIdAndStatus(5L, ReviewStatus.VISIBLE)).thenReturn(3L);
        when(reviewRepository.averageRating(5L, ReviewStatus.VISIBLE)).thenReturn(4.3333);
        when(reviewRepository.findByProductIdAndStatus(
                eq(5L), eq(ReviewStatus.VISIBLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review(40L, 5, ReviewStatus.VISIBLE))));

        ProductReviewsResponse response = service.productReviews(5L, 0, 10);

        assertThat(response.averageRating()).isEqualTo(4.3);
        assertThat(response.reviewCount()).isEqualTo(3L);
        assertThat(response.reviews().content()).hasSize(1);
    }

    @Test
    void productReviewsReturnsZeroWhenThereAreNoVisibleReviews() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5L, CatalogStatus.ACTIVE)));
        when(reviewRepository.countByProductIdAndStatus(5L, ReviewStatus.VISIBLE)).thenReturn(0L);
        when(reviewRepository.findByProductIdAndStatus(
                eq(5L), eq(ReviewStatus.VISIBLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ProductReviewsResponse response = service.productReviews(5L, 0, 10);

        assertThat(response.averageRating()).isEqualTo(0.0);
        assertThat(response.reviewCount()).isZero();
        verify(reviewRepository, never()).averageRating(any(), any());
    }

    @Test
    void productReviewsHidesInactiveProducts() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product(5L, CatalogStatus.INACTIVE)));

        assertThatThrownBy(() -> service.productReviews(5L, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void productReviewsRejectsUnknownProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.productReviews(99L, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------- admin moderation ----------------

    @Test
    void setStatusHidesAndUnhidesAReview() {
        Review review = review(40L, 4, ReviewStatus.VISIBLE);
        when(reviewRepository.findById(40L)).thenReturn(Optional.of(review));

        AdminReviewResponse hidden = service.setStatus(40L, ReviewStatus.HIDDEN);
        assertThat(hidden.status()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(hidden.customerEmail()).isEqualTo("customer@example.com");

        AdminReviewResponse visible = service.setStatus(40L, ReviewStatus.VISIBLE);
        assertThat(visible.status()).isEqualTo(ReviewStatus.VISIBLE);
    }

    @Test
    void setStatusRejectsUnknownReview() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setStatus(99L, ReviewStatus.HIDDEN))
                .isInstanceOf(ResourceNotFoundException.class);
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

    private OrderItem orderItem(Long id, OrderStatus orderStatus) {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 10L);
        order.setUser(user);
        order.setStatus(orderStatus);

        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", 20L);
        variant.setProduct(product(5L, CatalogStatus.ACTIVE));

        OrderItem item = new OrderItem();
        ReflectionTestUtils.setField(item, "id", id);
        item.setOrder(order);
        item.setVariant(variant);
        return item;
    }

    private Review review(Long id, int rating, ReviewStatus status) {
        Review review = new Review();
        ReflectionTestUtils.setField(review, "id", id);
        review.setUser(user);
        review.setProduct(product(5L, CatalogStatus.ACTIVE));
        review.setOrderItem(orderItem(30L, OrderStatus.DELIVERED));
        review.setRating(rating);
        review.setStatus(status);
        return review;
    }
}
