package com.ekup.fintech.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateWalletException extends DomainException {
	public DuplicateWalletException(String message) {
		super(message);
	}
}
