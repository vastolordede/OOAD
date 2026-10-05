package com.ooad.cosmetics.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void generatedTokenCanBeValidated() {
        JwtService jwtService = new JwtService(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                60_000
        );

        UserDetails userDetails = User.withUsername("user@example.com")
                .password("ignored")
                .roles("CUSTOMER")
                .build();

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(token))
                .isEqualTo("user@example.com");
        assertThat(jwtService.isValid(token, userDetails)).isTrue();
    }
}
