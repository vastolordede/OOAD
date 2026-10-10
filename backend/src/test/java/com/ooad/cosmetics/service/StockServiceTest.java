package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.repository.VariantStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockServiceTest {

    private VariantStockRepository stockRepository;
    private StockService service;

    @BeforeEach
    void setUp() {
        stockRepository = mock(VariantStockRepository.class);
        service = new StockService(stockRepository);
    }

    @Test
    void deductSucceedsWhenRepositoryUpdatedOneRow() {
        when(stockRepository.decreaseStock(eq(10L), eq(3), any(Instant.class))).thenReturn(1);

        assertThatCode(() -> service.deduct(10L, 3)).doesNotThrowAnyException();
    }

    @Test
    void deductRejectsWhenStockIsInsufficient() {
        when(stockRepository.decreaseStock(eq(10L), eq(3), any(Instant.class))).thenReturn(0);
        when(stockRepository.existsById(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.deduct(10L, 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient stock for variant 10");
    }

    @Test
    void deductReportsNotFoundWhenVariantDoesNotExist() {
        when(stockRepository.decreaseStock(eq(99L), eq(1), any(Instant.class))).thenReturn(0);
        when(stockRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deduct(99L, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deductRejectsNonPositiveQuantityWithoutTouchingDatabase() {
        assertThatThrownBy(() -> service.deduct(10L, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Quantity must be greater than 0");

        verify(stockRepository, never()).decreaseStock(anyLong(), anyInt(), any());
    }

    @Test
    void restoreSucceedsWhenRepositoryUpdatedOneRow() {
        when(stockRepository.increaseStock(eq(10L), eq(2), any(Instant.class))).thenReturn(1);

        assertThatCode(() -> service.restore(10L, 2)).doesNotThrowAnyException();
    }

    @Test
    void restoreReportsNotFoundWhenVariantDoesNotExist() {
        when(stockRepository.increaseStock(eq(99L), eq(2), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> service.restore(99L, 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
