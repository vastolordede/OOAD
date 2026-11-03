package com.ooad.cosmetics.service;

import com.ooad.cosmetics.controller.AdminProductController;
import com.ooad.cosmetics.dto.product.ProductDetailResponse;
import com.ooad.cosmetics.entity.Brand;
import com.ooad.cosmetics.entity.Category;
import com.ooad.cosmetics.entity.Product;
import com.ooad.cosmetics.repository.ProductImageRepository;
import com.ooad.cosmetics.repository.ProductRepository;
import com.ooad.cosmetics.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminProductDetailUnitTest {

    @Test
    void adminDetailUsesAllVariantsRepositoryInsteadOfActiveOnlyQuery() {
        ProductRepository products = mock(ProductRepository.class);
        ProductVariantRepository variants = mock(ProductVariantRepository.class);
        ProductImageRepository images = mock(ProductImageRepository.class);
        CatalogService catalogs = mock(CatalogService.class);
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(9L);
        when(product.getCategory()).thenReturn(mock(Category.class));
        when(product.getBrand()).thenReturn(mock(Brand.class));
        when(products.findById(9L)).thenReturn(Optional.of(product));
        when(variants.findByProductIdOrderByIdAsc(9L)).thenReturn(List.of());
        when(images.findByProductIdOrderByPrimaryImageDescSortOrderAscIdAsc(9L))
                .thenReturn(List.of());

        ProductService service = new ProductService(products, variants, images, catalogs);
        ProductDetailResponse detail = service.adminDetail(9L);

        assertEquals(9L, detail.id());
        verify(variants).findByProductIdOrderByIdAsc(9L);
        verify(variants, never()).findByProductIdAndStatusOrderByIdAsc(anyLong(), any());
    }

    @Test
    void adminControllerDelegatesDetailToService() {
        ProductService service = mock(ProductService.class);
        ProductDetailResponse expected = mock(ProductDetailResponse.class);
        when(service.adminDetail(9L)).thenReturn(expected);

        var response = new AdminProductController(service).productDetail(9L);

        assertTrue(response.success());
        assertSame(expected, response.data());
        verify(service).adminDetail(9L);
    }
}
