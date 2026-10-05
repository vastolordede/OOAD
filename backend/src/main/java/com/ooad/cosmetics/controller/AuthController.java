package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.dto.auth.AuthResponse;
import com.ooad.cosmetics.dto.auth.LoginRequest;
import com.ooad.cosmetics.dto.auth.RegisterRequest;
import com.ooad.cosmetics.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Account registered successfully",
                        authService.register(request)
                ));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ApiResponse.success(
                "Login successful",
                authService.login(request)
        );
    }
}
