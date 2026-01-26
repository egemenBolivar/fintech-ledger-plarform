package com.ekup.fintech.shared.exception;

public class WalletSuspendedException extends DomainException {
	public WalletSuspendedException(String message) {
		super(message);
	}
}
