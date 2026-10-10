package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.ProductVariant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Cập nhật tồn kho bằng MỘT câu UPDATE nguyên tử (atomic) ở database.
 *
 * Không đọc stock lên Java rồi ghi lại, nên hai request mua cùng lúc không thể
 * làm tồn kho âm: điều kiện "stock >= quantity" được kiểm tra ngay trong câu UPDATE.
 *
 * Lưu ý: không dùng clearAutomatically để không làm "detach" các entity khác
 * (ví dụ Order đang dựng) trong cùng transaction. Hệ quả: entity ProductVariant
 * đã load trước đó sẽ giữ giá trị stock cũ trong bộ nhớ, đừng đọc stock từ nó
 * sau khi gọi các hàm này.
 */
public interface VariantStockRepository extends Repository<ProductVariant, Long> {

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ProductVariant v
               SET v.stock = v.stock - :quantity,
                   v.version = v.version + 1,
                   v.updatedAt = :now
             WHERE v.id = :variantId
               AND v.stock >= :quantity
            """)
    int decreaseStock(
            @Param("variantId") Long variantId,
            @Param("quantity") int quantity,
            @Param("now") Instant now
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ProductVariant v
               SET v.stock = v.stock + :quantity,
                   v.version = v.version + 1,
                   v.updatedAt = :now
             WHERE v.id = :variantId
            """)
    int increaseStock(
            @Param("variantId") Long variantId,
            @Param("quantity") int quantity,
            @Param("now") Instant now
    );

    boolean existsById(Long id);
}
