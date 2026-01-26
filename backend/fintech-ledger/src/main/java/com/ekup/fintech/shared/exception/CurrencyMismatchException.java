package com.ekup.fintech.shared.exception;

public class CurrencyMismatchException extends DomainException {
	public CurrencyMismatchException(String message) {
		super(message);
	}
}
