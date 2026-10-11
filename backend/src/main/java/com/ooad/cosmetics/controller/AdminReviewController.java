package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.review.AdminReviewResponse;
import com.ooad.cosmetics.entity.ReviewStatus;
import com.ooad.cosmetics.service.ReviewService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@Validated
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminReviewResponse>> list(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(reviewService.adminList(status, productId, page, size));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminReviewResponse> status(
            @PathVariable Long id,
            @RequestParam ReviewStatus status
    ) {
        return ApiResponse.success(
                "Review status updated",
                reviewService.setStatus(id, status)
        );
    }
}
