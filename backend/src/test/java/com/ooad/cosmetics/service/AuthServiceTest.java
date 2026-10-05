package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.dto.auth.RegisterRequest;
import com.ooad.cosmetics.repository.UserRepository;
import com.ooad.cosmetics.security.CustomUserDetailsService;
import com.ooad.cosmetics.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void registerRejectsDuplicateEmail() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = mock(JwtService.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);

        when(userRepository.existsByEmailIgnoreCase("user@example.com"))
                .thenReturn(true);

        AuthService service = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                userDetailsService
        );

        RegisterRequest request = new RegisterRequest(
                "USER@example.com",
                "password123",
                "User",
                null
        );

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email is already registered");
    }
}
