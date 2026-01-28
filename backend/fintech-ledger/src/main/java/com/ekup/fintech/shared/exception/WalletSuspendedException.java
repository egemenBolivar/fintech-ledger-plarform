package com.ekup.fintech.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class WalletSuspendedException extends DomainException {
	public WalletSuspendedException(String message) {
		super(message);
	}
}
