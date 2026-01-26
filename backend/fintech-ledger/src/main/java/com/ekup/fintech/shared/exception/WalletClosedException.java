package com.ekup.fintech.shared.exception;

public class WalletClosedException extends DomainException {
	public WalletClosedException(String message) {
		super(message);
	}
}
