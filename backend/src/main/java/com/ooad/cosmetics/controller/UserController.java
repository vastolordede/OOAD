package com.ooad.cosmetics.controller;

import com.ooad.cosmetics.common.response.ApiResponse;
import com.ooad.cosmetics.dto.user.ChangePasswordRequest;
import com.ooad.cosmetics.dto.user.UpdateProfileRequest;
import com.ooad.cosmetics.dto.user.UserResponse;
import com.ooad.cosmetics.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success(userService.currentUser());
    }

    @PutMapping
    public ApiResponse<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success(
                "Profile updated",
                userService.updateProfile(request)
        );
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(request);
        return ApiResponse.success("Password changed");
    }
}
