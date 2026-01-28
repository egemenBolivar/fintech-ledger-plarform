package com.ekup.fintech.auth.application;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ekup.fintech.auth.api.dto.AuthRequest;
import com.ekup.fintech.auth.api.dto.AuthResponse;
import com.ekup.fintech.auth.api.dto.RegisterRequest;
import com.ekup.fintech.auth.domain.Role;
import com.ekup.fintech.auth.domain.User;
import com.ekup.fintech.auth.infrastructure.UserRepository;
import com.ekup.fintech.shared.exception.DuplicateResourceException;
import com.ekup.fintech.shared.exception.InvalidCredentialsException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager);
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("should register new user successfully")
        void shouldRegisterSuccessfully() {
            // Given
            RegisterRequest request = new RegisterRequest("test@example.com", "password", "Test User");
            
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

            // When
            AuthResponse response = authService.register(request);

            // Then
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.email()).isEqualTo(request.email());
            assertThat(response.fullName()).isEqualTo(request.fullName());
            
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            RegisterRequest request = new RegisterRequest("existing@example.com", "password", "Test User");
            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");
        }
    }

    @Nested
    @DisplayName("authenticate")
    class AuthenticateTests {

        @Test
        @DisplayName("should authenticate user successfully")
        void shouldAuthenticateSuccessfully() {
            // Given
            AuthRequest request = new AuthRequest("test@example.com", "password");
            User user = User.create(request.email(), "encoded-password", "Test User", Set.of(Role.USER));
            
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

            // When
            AuthResponse response = authService.authenticate(request);

            // Then
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.email()).isEqualTo(request.email());
            
            verify(userRepository).save(any(User.class)); // recordLogin
        }

        @Test
        @DisplayName("should throw InvalidCredentialsException for wrong password")
        void shouldThrowExceptionForWrongPassword() {
            // Given
            AuthRequest request = new AuthRequest("test@example.com", "wrong-password");
            
            when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

            // When/Then
            assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            String refreshToken = "valid-refresh-token";
            User user = User.create("test@example.com", "encoded", "Test", Set.of(Role.USER));
            
            when(jwtService.extractUsername(refreshToken)).thenReturn(user.getEmail());
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(jwtService.isTokenValid(refreshToken, user)).thenReturn(true);
            when(jwtService.generateToken(user)).thenReturn("new-access-token");

            // When
            AuthResponse response = authService.refreshToken(refreshToken);

            // Then
            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo(refreshToken);
        }

        @Test
        @DisplayName("should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidToken() {
            // Given
            String refreshToken = "invalid-token";
            User user = User.create("test@example.com", "encoded", "Test", Set.of(Role.USER));
            
            when(jwtService.extractUsername(refreshToken)).thenReturn(user.getEmail());
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(jwtService.isTokenValid(refreshToken, user)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid or expired refresh token");
        }
    }
}
