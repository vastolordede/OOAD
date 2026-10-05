package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.dto.product.ProductSort;
import com.ooad.cosmetics.repository.ProductImageRepository;
import com.ooad.cosmetics.repository.ProductRepository;
import com.ooad.cosmetics.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ProductServiceValidationTest {

    @Test
    void publicProductsRejectsInvalidPriceRangeBeforeQuery() {
        ProductService service = new ProductService(
                mock(ProductRepository.class),
                mock(ProductVariantRepository.class),
                mock(ProductImageRepository.class),
                mock(CatalogService.class)
        );

        assertThatThrownBy(() ->
                service.publicProducts(
                        null,
                        null,
                        null,
                        new BigDecimal("100"),
                        new BigDecimal("50"),
                        ProductSort.NEWEST,
                        0,
                        20
                )
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("minPrice must not exceed maxPrice");
    }
}
