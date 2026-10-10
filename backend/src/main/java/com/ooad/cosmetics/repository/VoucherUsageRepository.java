package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    boolean existsByVoucherIdAndUserId(Long voucherId, Long userId);

    Optional<VoucherUsage> findByOrderId(Long orderId);
}
