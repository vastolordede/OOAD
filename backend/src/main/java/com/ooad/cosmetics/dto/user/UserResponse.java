package com.ooad.cosmetics.dto.user;

import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.entity.UserRole;
import com.ooad.cosmetics.entity.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
