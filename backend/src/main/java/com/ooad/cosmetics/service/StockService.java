package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.repository.VariantStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Trừ / hoàn tồn kho. Phải được gọi bên trong một transaction có sẵn
 * (ví dụ lúc tạo đơn hoặc hủy đơn) để lỗi ở bước sau sẽ rollback cả việc trừ kho.
 */
@Service
public class StockService {

    private final VariantStockRepository stockRepository;

    public StockService(VariantStockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deduct(Long variantId, int quantity) {
        requirePositive(quantity);

        int updated = stockRepository.decreaseStock(variantId, quantity, Instant.now());
        if (updated == 0) {
            if (!stockRepository.existsById(variantId)) {
                throw new ResourceNotFoundException("ProductVariant", variantId);
            }
            throw new BadRequestException("Insufficient stock for variant " + variantId);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restore(Long variantId, int quantity) {
        requirePositive(quantity);

        int updated = stockRepository.increaseStock(variantId, quantity, Instant.now());
        if (updated == 0) {
            throw new ResourceNotFoundException("ProductVariant", variantId);
        }
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }
    }
}
