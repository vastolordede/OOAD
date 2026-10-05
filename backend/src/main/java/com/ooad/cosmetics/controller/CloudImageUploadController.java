package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.dto.product.CloudImageUploadResponse;
import com.ooad.cosmetics.service.CloudinaryImageStorageService;
import jakarta.validation.constraints.Min;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/products")
@Validated
@ConditionalOnProperty(
        name = "cloudinary.enabled",
        havingValue = "true"
)
public class CloudImageUploadController {

    private final CloudinaryImageStorageService imageStorageService;

    public CloudImageUploadController(
            CloudinaryImageStorageService imageStorageService
    ) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping("/{productId}/images/upload")
    public ApiResponse<CloudImageUploadResponse> upload(
            @PathVariable Long productId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "0") @Min(0) int sortOrder
    ) {
        return ApiResponse.success(
                "Image uploaded",
                imageStorageService.uploadProductImage(
                        productId,
                        file,
                        sortOrder
                )
        );
    }
}
