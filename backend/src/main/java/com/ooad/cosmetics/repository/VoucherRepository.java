package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.CatalogStatus;
import com.ooad.cosmetics.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    @Query("""
            SELECT v
            FROM Voucher v
            WHERE (:q IS NULL OR :q = '' OR LOWER(v.code) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR v.status = :status)
            """)
    Page<Voucher> search(
            @Param("q") String q,
            @Param("status") CatalogStatus status,
            Pageable pageable
    );

    /**
     * Tăng used_count nguyên tử. Trả về 0 nếu voucher đã hết lượt dùng,
     * nên hai đơn cùng lúc không thể vượt usage_limit.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Voucher v
               SET v.usedCount = v.usedCount + 1,
                   v.updatedAt = :now
             WHERE v.id = :id
               AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)
            """)
    int increaseUsedCount(@Param("id") Long id, @Param("now") Instant now);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Voucher v
               SET v.usedCount = v.usedCount - 1,
                   v.updatedAt = :now
             WHERE v.id = :id
               AND v.usedCount > 0
            """)
    int decreaseUsedCount(@Param("id") Long id, @Param("now") Instant now);
}
