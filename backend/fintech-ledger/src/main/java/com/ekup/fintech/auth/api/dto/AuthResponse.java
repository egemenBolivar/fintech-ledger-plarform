package com.ekup.fintech.auth.api.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String email,
    String fullName
) {}
