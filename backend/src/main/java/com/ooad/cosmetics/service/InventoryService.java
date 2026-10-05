package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.product.ProductVariantResponse;
import com.ooad.cosmetics.dto.product.StockUpdateRequest;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.ProductVariant;
import com.ooad.cosmetics.repository.ProductVariantRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ProductVariantRepository variantRepository;

    public InventoryService(ProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    @Transactional
    public ProductVariantResponse updateStock(Long variantId, StockUpdateRequest request) {
        ProductVariant variant = requireVariant(variantId);
        variant.setStock(request.stock());
        return ProductVariantResponse.from(variant);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductVariantResponse> lowStock(
            int threshold,
            int page,
            int size
    ) {
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "stock")
                        .and(Sort.by(Sort.Direction.ASC, "id"))
        );

        return PageResponse.from(
                variantRepository.findByStatusAndStockLessThanEqual(
                        CatalogStatus.ACTIVE,
                        threshold,
                        pageable
                ).map(ProductVariantResponse::from)
        );
    }

    private ProductVariant requireVariant(Long id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", id));
    }
}
