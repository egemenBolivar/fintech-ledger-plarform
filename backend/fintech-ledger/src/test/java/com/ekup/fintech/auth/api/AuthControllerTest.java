package com.ekup.fintech.auth.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ekup.fintech.auth.api.dto.AuthResponse;
import com.ekup.fintech.auth.application.AuthService;
import com.ekup.fintech.auth.infrastructure.TestSecurityConfig;
import com.ekup.fintech.shared.api.GlobalExceptionHandler;
import com.ekup.fintech.shared.exception.DuplicateResourceException;
import com.ekup.fintech.shared.exception.InvalidCredentialsException;

@WebMvcTest(AuthController.class)
@AutoConfigureJson
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@TestPropertySource(properties = "fintech.security.jwt.enabled=false")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private static final String BASE_URL = "/api/v1/auth";

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("should register new user successfully")
        void shouldRegisterSuccessfully() throws Exception {
            // Given
            AuthResponse response = new AuthResponse(
                "access-token",
                "refresh-token",
                "test@example.com",
                "Test User"
            );
            when(authService.register(any())).thenReturn(response);

            String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "password123",
                    "fullName": "Test User"
                }
                """;

            // When/Then
            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));
        }

        @Test
        @DisplayName("should return 409 when email already exists")
        void shouldReturn409WhenEmailExists() throws Exception {
            // Given
            when(authService.register(any()))
                .thenThrow(new DuplicateResourceException("Email already registered"));

            String requestBody = """
                {
                    "email": "existing@example.com",
                    "password": "password123",
                    "fullName": "Test User"
                }
                """;

            // When/Then
            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            String requestBody = """
                {
                    "email": "invalid-email",
                    "password": "password123",
                    "fullName": "Test User"
                }
                """;

            // When/Then
            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password too short")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "12345",
                    "fullName": "Test User"
                }
                """;

            // When/Then
            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("should login successfully")
        void shouldLoginSuccessfully() throws Exception {
            // Given
            AuthResponse response = new AuthResponse(
                "access-token",
                "refresh-token",
                "test@example.com",
                "Test User"
            );
            when(authService.authenticate(any())).thenReturn(response);

            String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;

            // When/Then
            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
        }

        @Test
        @DisplayName("should return 401 for invalid credentials")
        void shouldReturn401ForInvalidCredentials() throws Exception {
            // Given
            when(authService.authenticate(any()))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "wrongpassword"
                }
                """;

            // When/Then
            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // Given
            AuthResponse response = new AuthResponse(
                "new-access-token",
                "refresh-token",
                "test@example.com",
                "Test User"
            );
            when(authService.refreshToken(any())).thenReturn(response);

            String requestBody = """
                {
                    "refreshToken": "valid-refresh-token"
                }
                """;

            // When/Then
            mockMvc.perform(post(BASE_URL + "/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
        }

        @Test
        @DisplayName("should return 401 for invalid refresh token")
        void shouldReturn401ForInvalidRefreshToken() throws Exception {
            // Given
            when(authService.refreshToken(any()))
                .thenThrow(new InvalidCredentialsException("Invalid refresh token"));

            String requestBody = """
                {
                    "refreshToken": "invalid-token"
                }
                """;

            // When/Then
            mockMvc.perform(post(BASE_URL + "/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isUnauthorized());
        }
    }
}
