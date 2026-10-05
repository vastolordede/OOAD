package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.product.*;
import com.ooad.cosmetics.entity.*;
import com.ooad.cosmetics.repository.ProductImageRepository;
import com.ooad.cosmetics.repository.ProductRepository;
import com.ooad.cosmetics.repository.ProductVariantRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CatalogService catalogService;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            ProductImageRepository imageRepository,
            CatalogService catalogService
    ) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.catalogService = catalogService;
    }

    @Transactional
    public ProductDetailResponse createProduct(ProductRequest request) {
        Product product = new Product();
        applyProduct(product, request);
        productRepository.save(product);
        return toDetail(product, false);
    }

    @Transactional
    public ProductDetailResponse updateProduct(Long id, ProductRequest request) {
        Product product = requireProduct(id);
        applyProduct(product, request);
        return toDetail(product, false);
    }

    @Transactional
    public ProductDetailResponse setProductStatus(Long id, CatalogStatus status) {
        Product product = requireProduct(id);
        product.setStatus(status);
        return toDetail(product, false);
    }

    @Transactional
    public ProductVariantResponse createVariant(
            Long productId,
            ProductVariantRequest request
    ) {
        Product product = requireProduct(productId);
        String sku = normalizeSku(request.sku());

        if (variantRepository.existsBySkuIgnoreCase(sku)) {
            throw new BadRequestException("SKU already exists");
        }

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setPrice(request.price());
        variant.setStock(request.stock());

        return ProductVariantResponse.from(variantRepository.save(variant));
    }

    @Transactional
    public ProductVariantResponse updateVariant(
            Long id,
            ProductVariantRequest request
    ) {
        ProductVariant variant = requireVariant(id);
        String sku = normalizeSku(request.sku());

        if (variantRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new BadRequestException("SKU already exists");
        }

        variant.setSku(sku);
        variant.setPrice(request.price());
        variant.setStock(request.stock());

        return ProductVariantResponse.from(variant);
    }

    @Transactional
    public ProductVariantResponse setVariantStatus(Long id, CatalogStatus status) {
        ProductVariant variant = requireVariant(id);
        variant.setStatus(status);
        return ProductVariantResponse.from(variant);
    }

    @Transactional
    public ProductImageResponse addImage(
            Long productId,
            ProductImageRequest request
    ) {
        Product product = requireProduct(productId);

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(request.imageUrl().trim());
        image.setSortOrder(request.sortOrder());

        if (imageRepository.countByProductId(productId) == 0) {
            image.setPrimaryImage(true);
        }

        return ProductImageResponse.from(imageRepository.save(image));
    }

    @Transactional
    public void removeImage(Long productId, Long imageId) {
        ProductImage image = requireImage(productId, imageId);
        boolean wasPrimary = image.isPrimaryImage();

        imageRepository.delete(image);
        imageRepository.flush();

        if (wasPrimary) {
            List<ProductImage> remaining =
                    imageRepository.findByProductIdOrderByPrimaryImageDescSortOrderAscIdAsc(productId);

            if (!remaining.isEmpty()) {
                remaining.get(0).setPrimaryImage(true);
            }
        }
    }

    @Transactional
    public ProductImageResponse setPrimaryImage(Long productId, Long imageId) {
        ProductImage target = requireImage(productId, imageId);

        List<ProductImage> images =
                imageRepository.findByProductIdOrderByPrimaryImageDescSortOrderAscIdAsc(productId);

        images.forEach(image -> image.setPrimaryImage(false));
        imageRepository.flush();

        target.setPrimaryImage(true);
        return ProductImageResponse.from(target);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> publicProducts(
            String q,
            Long categoryId,
            Long brandId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductSort sort,
            int page,
            int size
    ) {
        validatePriceRange(minPrice, maxPrice);

        ProductSort effectiveSort = sort == null ? ProductSort.NEWEST : sort;
        PageRequest pageable = PageRequest.of(page, size);

        return PageResponse.from(
                productRepository.searchPublic(
                        normalizeQuery(q),
                        categoryId,
                        brandId,
                        minPrice,
                        maxPrice,
                        effectiveSort.name(),
                        pageable
                ).map(this::toSummary)
        );
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse publicDetail(Long id) {
        Product product = requireProduct(id);

        if (product.getStatus() != CatalogStatus.ACTIVE
                || product.getCategory().getStatus() != CatalogStatus.ACTIVE
                || product.getBrand().getStatus() != CatalogStatus.ACTIVE) {
            throw new ResourceNotFoundException("Product", id);
        }

        return toDetail(product, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> adminProducts(
            String q,
            CatalogStatus status,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        return PageResponse.from(
                productRepository.searchAdmin(
                        normalizeQuery(q),
                        status,
                        pageable
                ).map(this::toSummary)
        );
    }

    public Product requireProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public ProductVariant requireVariant(Long id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", id));
    }

    private ProductImage requireImage(Long productId, Long imageId) {
        return imageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));
    }

    private void applyProduct(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(normalizeNullable(request.description()));
        product.setCategory(catalogService.requireCategory(request.categoryId()));
        product.setBrand(catalogService.requireBrand(request.brandId()));
    }

    private ProductSummaryResponse toSummary(Product product) {
        List<ProductImage> images =
                imageRepository.findByProductIdOrderByPrimaryImageDescSortOrderAscIdAsc(product.getId());

        String primaryImageUrl = images.isEmpty()
                ? null
                : images.get(0).getImageUrl();

        return new ProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getStatus(),
                variantRepository.findMinActivePrice(product.getId()),
                primaryImageUrl,
                product.getCreatedAt()
        );
    }

    private ProductDetailResponse toDetail(Product product, boolean publicOnly) {
        List<ProductVariant> variants = publicOnly
                ? variantRepository.findByProductIdAndStatusOrderByIdAsc(
                        product.getId(),
                        CatalogStatus.ACTIVE
                )
                : variantRepository.findByProductIdOrderByIdAsc(product.getId());

        List<ProductImage> images =
                imageRepository.findByProductIdOrderByPrimaryImageDescSortOrderAscIdAsc(product.getId());

        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getStatus(),
                variantRepository.findMinActivePrice(product.getId()),
                variants.stream().map(ProductVariantResponse::from).toList(),
                images.stream().map(ProductImageResponse::from).toList(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }

    private String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String value = q.trim();
        return value.isEmpty() ? null : value;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.signum() < 0) {
            throw new BadRequestException("minPrice must be greater than or equal to 0");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new BadRequestException("maxPrice must be greater than or equal to 0");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must not exceed maxPrice");
        }
    }
}
