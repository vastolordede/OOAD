package com.ooad.cosmetics.dto.review;

import com.ooad.cosmetics.entity.Review;
import com.ooad.cosmetics.entity.ReviewStatus;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long productId,
        Long orderItemId,
        String reviewerName,
        int rating,
        String comment,
        ReviewStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getOrderItem().getId(),
                maskName(review.getUser().getFullName()),
                review.getRating(),
                review.getComment(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    /** "Nguyen Van A" -> "Nguyen A." để không lộ họ tên đầy đủ trên trang công khai. */
    static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Customer";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        String last = parts[parts.length - 1];
        return parts[0] + " " + last.charAt(0) + ".";
    }
}
