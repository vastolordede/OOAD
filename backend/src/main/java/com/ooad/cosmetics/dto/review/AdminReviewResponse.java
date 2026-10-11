package com.ooad.cosmetics.dto.review;

import com.ooad.cosmetics.entity.Review;
import com.ooad.cosmetics.entity.ReviewStatus;

import java.time.Instant;

public record AdminReviewResponse(
        Long id,
        Long productId,
        String productName,
        Long orderItemId,
        Long customerId,
        String customerEmail,
        String customerName,
        int rating,
        String comment,
        ReviewStatus status,
        Instant createdAt
) {
    public static AdminReviewResponse from(Review review) {
        return new AdminReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getProduct().getName(),
                review.getOrderItem().getId(),
                review.getUser().getId(),
                review.getUser().getEmail(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getCreatedAt()
        );
    }
}
