package com.ekup.fintech.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CurrencyMismatchException extends DomainException {
	public CurrencyMismatchException(String message) {
		super(message);
	}
}
