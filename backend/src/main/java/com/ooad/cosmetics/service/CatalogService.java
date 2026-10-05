package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.catalog.BrandResponse;
import com.ooad.cosmetics.dto.catalog.CatalogNameRequest;
import com.ooad.cosmetics.dto.catalog.CategoryResponse;
import com.ooad.cosmetics.entity.Brand;
import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.Category;
import com.ooad.cosmetics.repository.BrandRepository;
import com.ooad.cosmetics.repository.CategoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public CatalogService(
            CategoryRepository categoryRepository,
            BrandRepository brandRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CatalogNameRequest request) {
        String name = normalizeName(request.name());

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Category name already exists");
        }

        Category category = new Category();
        category.setName(name);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CatalogNameRequest request) {
        Category category = requireCategory(id);
        String name = normalizeName(request.name());

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BadRequestException("Category name already exists");
        }

        category.setName(name);
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse setCategoryStatus(Long id, CatalogStatus status) {
        Category category = requireCategory(id);
        category.setStatus(status);
        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> adminCategories(int page, int size) {
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "name")
        );
        return PageResponse.from(
                categoryRepository.findAll(pageable).map(CategoryResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> publicCategories() {
        return categoryRepository.findByStatusOrderByNameAsc(CatalogStatus.ACTIVE)
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public BrandResponse createBrand(CatalogNameRequest request) {
        String name = normalizeName(request.name());

        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Brand name already exists");
        }

        Brand brand = new Brand();
        brand.setName(name);
        return BrandResponse.from(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse updateBrand(Long id, CatalogNameRequest request) {
        Brand brand = requireBrand(id);
        String name = normalizeName(request.name());

        if (brandRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BadRequestException("Brand name already exists");
        }

        brand.setName(name);
        return BrandResponse.from(brand);
    }

    @Transactional
    public BrandResponse setBrandStatus(Long id, CatalogStatus status) {
        Brand brand = requireBrand(id);
        brand.setStatus(status);
        return BrandResponse.from(brand);
    }

    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> adminBrands(int page, int size) {
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "name")
        );
        return PageResponse.from(
                brandRepository.findAll(pageable).map(BrandResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> publicBrands() {
        return brandRepository.findByStatusOrderByNameAsc(CatalogStatus.ACTIVE)
                .stream()
                .map(BrandResponse::from)
                .toList();
    }

    public Category requireCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    public Brand requireBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", id));
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
