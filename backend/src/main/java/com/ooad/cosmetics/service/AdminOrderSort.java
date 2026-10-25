package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Chuyển tham số sortBy/direction của Admin Order list thành Sort, chỉ cho phép field trong whitelist. */
public final class AdminOrderSort {

    private static final Set<String> ALLOWED = Set.of("createdAt", "totalAmount", "status", "id");

    private AdminOrderSort() {}

    public static Sort resolve(String sortBy, String direction) {
        String field = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        if (!ALLOWED.contains(field)) {
            throw new BadRequestException("sortBy must be one of: createdAt, totalAmount, status, id");
        }

        Sort.Direction dir;
        if (direction == null || direction.isBlank() || "desc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.DESC;
        } else if ("asc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.ASC;
        } else {
            throw new BadRequestException("direction must be asc or desc");
        }

        Sort sort = Sort.by(dir, field);
        // Thêm id để thứ tự ổn định khi nhiều đơn trùng giá trị sort.
        return field.equals("id") ? sort : sort.and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
