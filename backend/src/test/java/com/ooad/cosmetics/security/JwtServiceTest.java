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

    @Test
    void changingPasswordInvalidatesPreviouslyIssuedJwt() {
        JwtService jwtService = new JwtService(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", 60_000
        );
        UserDetails before = User.withUsername("user@example.com")
                .password("bcrypt-hash-before").roles("CUSTOMER").build();
        UserDetails after = User.withUsername("user@example.com")
                .password("bcrypt-hash-after").roles("CUSTOMER").build();

        String token = jwtService.generateToken(before);

        assertThat(jwtService.isValid(token, before)).isTrue();
        assertThat(jwtService.isValid(token, after)).isFalse();
    }
}
