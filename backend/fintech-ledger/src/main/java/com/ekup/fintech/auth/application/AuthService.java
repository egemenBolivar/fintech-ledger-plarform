package com.ekup.fintech.auth.application;

import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ekup.fintech.auth.api.dto.AuthRequest;
import com.ekup.fintech.auth.api.dto.AuthResponse;
import com.ekup.fintech.auth.api.dto.RegisterRequest;
import com.ekup.fintech.auth.domain.Role;
import com.ekup.fintech.auth.domain.User;
import com.ekup.fintech.auth.infrastructure.UserRepository;
import com.ekup.fintech.shared.exception.DuplicateResourceException;
import com.ekup.fintech.shared.exception.InvalidCredentialsException;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        
        User user = User.create(
            request.email(),
            passwordEncoder.encode(request.password()),
            request.fullName(),
            Set.of(Role.USER)
        );
        
        userRepository.save(user);
        
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getFullName());
    }
    
    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        
        user.recordLogin();
        userRepository.save(user);
        
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getFullName());
    }
    
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        String userEmail = jwtService.extractUsername(refreshToken);
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));
        
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }
        
        String newAccessToken = jwtService.generateToken(user);
        
        return new AuthResponse(newAccessToken, refreshToken, user.getEmail(), user.getFullName());
    }
}
