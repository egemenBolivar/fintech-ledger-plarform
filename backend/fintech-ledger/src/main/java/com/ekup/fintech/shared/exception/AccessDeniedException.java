package com.ekup.fintech.shared.exception;

public class AccessDeniedException extends DomainException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
