package com.ekup.fintech.shared.exception;

public class InsufficientBalanceException extends DomainException {
	public InsufficientBalanceException(String message) {
		super(message);
	}
}
