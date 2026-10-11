package com.ooad.cosmetics.dto.review;

import com.ooad.cosmetics.common.response.PageResponse;

public record ProductReviewsResponse(
        double averageRating,
        long reviewCount,
        PageResponse<ReviewResponse> reviews
) {
}
