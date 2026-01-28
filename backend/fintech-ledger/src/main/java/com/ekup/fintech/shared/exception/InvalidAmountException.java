package com.ekup.fintech.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAmountException extends DomainException {
	public InvalidAmountException(String message) {
		super(message);
	}
}
