package com.ooad.cosmetics.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ExternalServiceException;
import com.ooad.cosmetics.dto.product.CloudImageUploadResponse;
import com.ooad.cosmetics.dto.product.ProductImageRequest;
import com.ooad.cosmetics.dto.product.ProductImageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@ConditionalOnProperty(
        name = "cloudinary.enabled",
        havingValue = "true"
)
public class CloudinaryImageStorageService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final ProductService productService;
    private final String folder;

    public CloudinaryImageStorageService(
            Cloudinary cloudinary,
            ProductService productService,
            @Value("${cloudinary.folder:ooad-cosmetics}") String folder
    ) {
        this.cloudinary = cloudinary;
        this.productService = productService;
        this.folder = folder;
    }

    public CloudImageUploadResponse uploadProductImage(
            Long productId,
            MultipartFile file,
            int sortOrder
    ) {
        validate(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder + "/products/" + productId,
                            "resource_type", "image"
                    )
            );

            String secureUrl = String.valueOf(result.get("secure_url"));
            String publicId = String.valueOf(result.get("public_id"));

            ProductImageResponse image = productService.addImage(
                    productId,
                    new ProductImageRequest(secureUrl, sortOrder)
            );

            return new CloudImageUploadResponse(
                    secureUrl,
                    publicId,
                    image
            );
        } catch (IOException | RuntimeException ex) {
            throw new ExternalServiceException(
                    "Cloud image upload failed",
                    ex
            );
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("Image file must not exceed 10 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }
    }
}
