package com.ooad.cosmetics.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForgotPasswordResponse(
        String message,
        String resetToken
) {
}
