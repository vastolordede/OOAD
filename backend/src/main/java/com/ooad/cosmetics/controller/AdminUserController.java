package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.user.UserResponse;
import com.ooad.cosmetics.entity.UserStatus;
import com.ooad.cosmetics.service.UserService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@Validated
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(userService.adminList(q, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(userService.adminDetail(id));
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<UserResponse> disable(@PathVariable Long id) {
        return ApiResponse.success(
                "User disabled",
                userService.setStatus(id, UserStatus.DISABLED)
        );
    }

    @PutMapping("/{id}/enable")
    public ApiResponse<UserResponse> enable(@PathVariable Long id) {
        return ApiResponse.success(
                "User enabled",
                userService.setStatus(id, UserStatus.ACTIVE)
        );
    }
}
