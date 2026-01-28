package com.ekup.fintech.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InsufficientBalanceException extends DomainException {
	public InsufficientBalanceException(String message) {
		super(message);
	}
}
