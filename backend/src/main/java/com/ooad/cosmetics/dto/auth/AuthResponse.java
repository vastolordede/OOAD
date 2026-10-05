package com.ooad.cosmetics.dto.auth;

import com.ooad.cosmetics.dto.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {
}
