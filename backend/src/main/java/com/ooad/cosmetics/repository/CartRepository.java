package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.Cart;
import com.ooad.cosmetics.entity.CartStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);

    /**
     * Khóa dòng giỏ hàng khi tạo đơn: nếu khách bấm "Đặt hàng" hai lần liên tiếp,
     * request thứ hai phải chờ request đầu xong rồi thấy giỏ đã trống.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId AND c.status = :status")
    Optional<Cart> lockByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") CartStatus status
    );
}
