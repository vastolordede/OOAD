package com.ooad.cosmetics.service;

import com.ooad.cosmetics.common.exception.BadRequestException;
import com.ooad.cosmetics.dto.auth.ForgotPasswordResponse;
import com.ooad.cosmetics.dto.auth.ResetPasswordRequest;
import com.ooad.cosmetics.entity.PasswordResetToken;
import com.ooad.cosmetics.entity.User;
import com.ooad.cosmetics.entity.UserStatus;
import com.ooad.cosmetics.repository.PasswordResetTokenRepository;
import com.ooad.cosmetics.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String GENERIC_MESSAGE =
            "If the email exists, password reset instructions have been created.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final long expirationMinutes;
    private final boolean exposeToken;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.password-reset.expiration-minutes:30}") long expirationMinutes,
            @Value("${app.password-reset.expose-token:false}") boolean exposeToken
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.expirationMinutes = expirationMinutes;
        this.exposeToken = exposeToken;
    }

    @Transactional
    public ForgotPasswordResponse requestReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();

        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);

        if (optionalUser.isEmpty()
                || optionalUser.get().getStatus() != UserStatus.ACTIVE) {
            return new ForgotPasswordResponse(GENERIC_MESSAGE, null);
        }

        User user = optionalUser.get();

        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();

        String rawToken = generateRawToken();

        PasswordResetToken entity = new PasswordResetToken();
        entity.setUser(user);
        entity.setTokenHash(hashToken(rawToken));
        entity.setExpiresAt(
                Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES)
        );

        tokenRepository.save(entity);

        // No mail subsystem is required by the current project scope.
        // For local/manual testing only, RESET_EXPOSE_TOKEN=true returns
        // the raw token. It remains false by default.
        return new ForgotPasswordResponse(
                GENERIC_MESSAGE,
                exposeToken ? rawToken : null
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Instant now = Instant.now();

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHash(hashToken(request.token()))
                .orElseThrow(() ->
                        new BadRequestException("Invalid or expired reset token")
                );

        if (resetToken.isUsed() || resetToken.isExpired(now)) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new BadRequestException(
                    "New password must be different from the current password"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );
        resetToken.setUsedAt(now);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(rawToken.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
