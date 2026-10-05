package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.common.exception.ResourceNotFoundException;
import com.ooad.cosmetics.common.response.PageResponse;
import com.ooad.cosmetics.dto.user.ChangePasswordRequest;
import com.ooad.cosmetics.dto.user.UpdateProfileRequest;
import com.ooad.cosmetics.dto.user.UserResponse;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.entity.UserStatus;
import com.ooad.cosmetics.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            CurrentUserService currentUserService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        return UserResponse.from(currentUserService.requireCurrentUser());
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUserService.requireCurrentUser();
        user.setFullName(request.fullName().trim());
        user.setPhone(normalizeNullable(request.phone()));
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserService.requireCurrentUser();

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new BadRequestException(
                    "New password must be different from the current password"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> adminList(
            String q,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<User> users;

        if (q == null || q.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            String keyword = q.trim();
            users = userRepository
                    .findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                            keyword,
                            keyword,
                            pageable
                    );
        }

        return PageResponse.from(users.map(UserResponse::from));
    }

    @Transactional(readOnly = true)
    public UserResponse adminDetail(Long id) {
        return UserResponse.from(requireUser(id));
    }

    @Transactional
    public UserResponse setStatus(Long id, UserStatus status) {
        User user = requireUser(id);
        user.setStatus(status);
        return UserResponse.from(user);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", id)
                );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
