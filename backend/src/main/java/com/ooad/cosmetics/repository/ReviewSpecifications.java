package com.ooad.cosmetics.repository;

import com.ooad.cosmetics.entity.Review;
import com.ooad.cosmetics.entity.ReviewStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** R13: bộ lọc review cho Admin kiểm duyệt. */
public final class ReviewSpecifications {

    private ReviewSpecifications() {
    }

    public static Specification<Review> adminFilter(ReviewStatus status, Long productId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
