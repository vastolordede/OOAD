package com.ooad.cosmetics.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequest(
        @Min(1)
        @Max(5)
        int rating,

        @Size(max = 2000)
        String comment
) {
}
