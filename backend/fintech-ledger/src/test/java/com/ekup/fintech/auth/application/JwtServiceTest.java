package com.ekup.fintech.auth.application;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.ekup.fintech.auth.domain.Role;
import com.ekup.fintech.auth.domain.User;

@SpringBootTest
@TestPropertySource(properties = {
    "fintech.jwt.secret=dGhpc2lzYXRlc3RzZWNyZXRrZXl0aGF0aXNsb25nZW5vdWdoZm9ydGVzdGluZw==",
    "fintech.jwt.expiration=3600000",
    "fintech.jwt.refresh-expiration=86400000"
})
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create("test@example.com", "encoded-password", "Test User", Set.of(Role.USER));
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("should generate valid access token")
        void shouldGenerateValidAccessToken() {
            // When
            String token = jwtService.generateToken(testUser);

            // Then
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtService.extractUsername(token)).isEqualTo(testUser.getEmail());
            assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
        }

        @Test
        @DisplayName("should generate valid refresh token")
        void shouldGenerateValidRefreshToken() {
            // When
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Then
            assertThat(refreshToken).isNotNull().isNotEmpty();
            assertThat(jwtService.extractUsername(refreshToken)).isEqualTo(testUser.getEmail());
            assertThat(jwtService.isTokenValid(refreshToken, testUser)).isTrue();
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsernameTests {

        @Test
        @DisplayName("should extract username from token")
        void shouldExtractUsername() {
            // Given
            String token = jwtService.generateToken(testUser);

            // When
            String username = jwtService.extractUsername(token);

            // Then
            assertThat(username).isEqualTo(testUser.getEmail());
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class TokenValidationTests {

        @Test
        @DisplayName("should return true for valid token and matching user")
        void shouldReturnTrueForValidToken() {
            // Given
            String token = jwtService.generateToken(testUser);

            // When
            boolean isValid = jwtService.isTokenValid(token, testUser);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("should return false for token with different user")
        void shouldReturnFalseForDifferentUser() {
            // Given
            String token = jwtService.generateToken(testUser);
            User differentUser = User.create("other@example.com", "password", "Other", Set.of(Role.USER));

            // When
            boolean isValid = jwtService.isTokenValid(token, differentUser);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("should throw exception for malformed token")
        void shouldThrowExceptionForMalformedToken() {
            // Given
            String malformedToken = "not.a.valid.token";

            // When/Then
            assertThatThrownBy(() -> jwtService.extractUsername(malformedToken))
                .isInstanceOf(Exception.class);
        }
    }
}
