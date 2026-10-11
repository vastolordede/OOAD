package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.Order;
import com.ooad.cosmetics.entity.OrderStatus;
import com.ooad.cosmetics.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** O25: bộ lọc đơn hàng cho Admin (trạng thái, từ khóa, khoảng ngày). */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    /**
     * @param q           tìm theo mã đơn, email khách, tên hoặc số điện thoại người nhận
     * @param from        tính từ thời điểm này (bao gồm)
     * @param toExclusive đến thời điểm này (không bao gồm)
     */
    public static Specification<Order> adminFilter(
            OrderStatus status,
            String q,
            Instant from,
            Instant toExclusive
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(root.<Instant>get("createdAt"), toExclusive));
            }

            if (q != null && !q.isBlank()) {
                String trimmed = q.trim();
                String term = trimmed.toLowerCase(Locale.ROOT);
                String like = "%" + term + "%";

                Join<Order, User> user = root.join("user", JoinType.LEFT);

                List<Predicate> anyOf = new ArrayList<>();
                anyOf.add(cb.like(cb.lower(user.<String>get("email")), like));
                anyOf.add(cb.like(cb.lower(root.<String>get("receiverName")), like));
                anyOf.add(cb.like(root.<String>get("receiverPhone"), "%" + trimmed + "%"));
                if (term.matches("\\d{1,18}")) {
                    anyOf.add(cb.equal(root.<Long>get("id"), Long.parseLong(term)));
                }
                predicates.add(cb.or(anyOf.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
