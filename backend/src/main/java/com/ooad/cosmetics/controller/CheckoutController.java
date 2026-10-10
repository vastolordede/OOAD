package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.dto.checkout.CheckoutPreviewRequest;
import com.ooad.cosmetics.dto.checkout.CheckoutPreviewResponse;
import com.ooad.cosmetics.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/preview")
    public ApiResponse<CheckoutPreviewResponse> preview(
            @Valid @RequestBody CheckoutPreviewRequest request
    ) {
        return ApiResponse.success(checkoutService.preview(request));
    }
}
