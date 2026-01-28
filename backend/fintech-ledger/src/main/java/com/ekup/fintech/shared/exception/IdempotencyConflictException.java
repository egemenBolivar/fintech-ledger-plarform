package com.ekup.fintech.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class IdempotencyConflictException extends DomainException {
	public IdempotencyConflictException(String message) {
		super(message);
	}
}
