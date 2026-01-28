package com.ekup.fintech.auth.domain;

public enum Role {
    USER,       // Normal user - can manage own wallets
    ADMIN       // Admin - can access admin endpoints
}
