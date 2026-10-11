package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.dto.review.ProductReviewsResponse;
import com.ooad.cosmetics.service.ReviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/products/{productId}/reviews")
@Validated
@SecurityRequirements
public class PublicReviewController {

    private final ReviewService reviewService;

    public PublicReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<ProductReviewsResponse> reviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(reviewService.productReviews(productId, page, size));
    }
}
