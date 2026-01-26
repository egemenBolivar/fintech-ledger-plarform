package com.ekup.fintech.shared.exception;

public class DuplicateWalletException extends DomainException {
	public DuplicateWalletException(String message) {
		super(message);
	}
}
