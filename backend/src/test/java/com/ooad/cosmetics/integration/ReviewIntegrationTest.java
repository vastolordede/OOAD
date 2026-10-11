package com.ooad.cosmetics.integration;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.dto.order.CreateOrderRequest;
import com.ooad.cosmetics.dto.order.OrderResponse;
import com.ooad.cosmetics.dto.review.CreateReviewRequest;
import com.ooad.cosmetics.dto.review.ProductReviewsResponse;
import com.ooad.cosmetics.dto.review.ReviewResponse;
import com.ooad.cosmetics.dto.review.UpdateReviewRequest;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.entity.ReviewStatus;
import com.ooad.cosmetics.service.CheckoutService;
import com.ooad.cosmetics.service.OrderService;
import com.ooad.cosmetics.service.ReviewService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** R14: điều kiện được review và cách tính điểm, trên PostgreSQL thật. */
@Tag("integration")
class ReviewIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReviewService reviewService;

    private OrderResponse placeOrder(String email, long variantId) {
        long addressId = prepareCustomerWithCart(email, variantId, 1);
        return checkoutService.createOrder(
                new CreateOrderRequest(addressId, null, PaymentMethod.COD, null)
        );
    }

    private OrderResponse deliveredOrder(String email, long variantId) {
        OrderResponse order = placeOrder(email, variantId);
        orderService.updateStatus(order.id(), OrderStatus.CONFIRMED);
        orderService.updateStatus(order.id(), OrderStatus.SHIPPING);
        orderService.updateStatus(order.id(), OrderStatus.DELIVERED);
        return order;
    }

    @Test
    void reviewIsRejectedUntilTheOrderIsDelivered() {
        long variantId = createVariant("100000.00", 5);
        OrderResponse order = placeOrder("buyer@example.com", variantId);
        long orderItemId = order.items().get(0).id();

        assertThatThrownBy(() -> reviewService.create(new CreateReviewRequest(orderItemId, 5, "Tot")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You can only review products from delivered orders");

        assertThat(count("SELECT COUNT(*) FROM reviews")).isZero();
    }

    @Test
    void aDeliveredItemCanBeReviewedExactlyOnce() {
        long variantId = createVariant("100000.00", 5);
        OrderResponse order = deliveredOrder("buyer@example.com", variantId);
        long orderItemId = order.items().get(0).id();

        ReviewResponse review = reviewService.create(new CreateReviewRequest(orderItemId, 4, "  Dung tot "));

        assertThat(review.rating()).isEqualTo(4);
        assertThat(review.comment()).isEqualTo("Dung tot");
        assertThat(review.productId()).isEqualTo(productIdOf(variantId));

        assertThatThrownBy(() -> reviewService.create(new CreateReviewRequest(orderItemId, 5, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This item has already been reviewed");
        assertThat(count("SELECT COUNT(*) FROM reviews")).isEqualTo(1);
    }

    @Test
    void aCustomerCannotReviewAnItemFromSomeoneElsesOrder() {
        long variantId = createVariant("100000.00", 5);
        OrderResponse order = deliveredOrder("owner@example.com", variantId);
        long orderItemId = order.items().get(0).id();

        createCustomer("stranger@example.com");
        authenticateAs("stranger@example.com", "CUSTOMER");

        assertThatThrownBy(() -> reviewService.create(new CreateReviewRequest(orderItemId, 5, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(count("SELECT COUNT(*) FROM reviews")).isZero();
    }

    @Test
    void averageRatingAndCountIgnoreHiddenReviews() {
        long variantId = createVariant("100000.00", 10);
        long productId = productIdOf(variantId);

        OrderResponse first = deliveredOrder("a@example.com", variantId);
        reviewService.create(new CreateReviewRequest(first.items().get(0).id(), 5, "Tuyet"));

        OrderResponse second = deliveredOrder("b@example.com", variantId);
        ReviewResponse lowReview =
                reviewService.create(new CreateReviewRequest(second.items().get(0).id(), 3, "Tam on"));

        ProductReviewsResponse both = reviewService.productReviews(productId, 0, 10);
        assertThat(both.reviewCount()).isEqualTo(2);
        assertThat(both.averageRating()).isEqualTo(4.0);
        assertThat(both.reviews().content()).hasSize(2);

        reviewService.setStatus(lowReview.id(), ReviewStatus.HIDDEN);
        ProductReviewsResponse onlyVisible = reviewService.productReviews(productId, 0, 10);
        assertThat(onlyVisible.reviewCount()).isEqualTo(1);
        assertThat(onlyVisible.averageRating()).isEqualTo(5.0);
        assertThat(onlyVisible.reviews().content()).hasSize(1);

        reviewService.setStatus(lowReview.id(), ReviewStatus.VISIBLE);
        assertThat(reviewService.productReviews(productId, 0, 10).averageRating()).isEqualTo(4.0);
    }

    @Test
    void productWithoutReviewsHasZeroAverageAndCount() {
        long variantId = createVariant("100000.00", 5);

        ProductReviewsResponse result = reviewService.productReviews(productIdOf(variantId), 0, 10);

        assertThat(result.reviewCount()).isZero();
        assertThat(result.averageRating()).isEqualTo(0.0);
        assertThat(result.reviews().content()).isEmpty();
    }

    @Test
    void ownerCanEditAndDeleteButNotOnceAModeratorHidAReview() {
        long variantId = createVariant("100000.00", 5);
        OrderResponse order = deliveredOrder("buyer@example.com", variantId);
        ReviewResponse review =
                reviewService.create(new CreateReviewRequest(order.items().get(0).id(), 3, "Binh thuong"));

        ReviewResponse edited = reviewService.update(review.id(), new UpdateReviewRequest(5, "Dung qua tot"));
        assertThat(edited.rating()).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT rating FROM reviews", Integer.class)).isEqualTo(5);

        reviewService.setStatus(review.id(), ReviewStatus.HIDDEN);
        assertThatThrownBy(() -> reviewService.update(review.id(), new UpdateReviewRequest(1, "x")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This review has been hidden by a moderator");
        assertThatThrownBy(() -> reviewService.delete(review.id()))
                .isInstanceOf(BadRequestException.class);
        assertThat(count("SELECT COUNT(*) FROM reviews")).isEqualTo(1);

        reviewService.setStatus(review.id(), ReviewStatus.VISIBLE);
        reviewService.delete(review.id());
        assertThat(count("SELECT COUNT(*) FROM reviews")).isZero();
    }
}
