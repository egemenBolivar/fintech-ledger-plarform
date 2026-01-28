package com.ekup.fintech.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.ekup.fintech.shared.exception.CurrencyMismatchException;
import com.ekup.fintech.shared.exception.InvalidAmountException;

public record Money(BigDecimal amount, Currency currency) {
	public static final int SCALE = 4;

	public Money {
		Objects.requireNonNull(amount, "amount");
		Objects.requireNonNull(currency, "currency");
		amount = normalize(amount);
	}

	public static Money zero(Currency currency) {
		return new Money(BigDecimal.ZERO, currency);
	}

	public static Money of(BigDecimal amount, Currency currency) {
		return new Money(amount, currency);
	}

	public static Money of(String amount, Currency currency) {
		return new Money(new BigDecimal(amount), currency);
	}

	public Money add(Money other) {
		requireSameCurrency(other);
		return new Money(this.amount.add(other.amount), this.currency);
	}

	public Money subtract(Money other) {
		requireSameCurrency(other);
		return new Money(this.amount.subtract(other.amount), this.currency);
	}

	public Money negate() {
		return new Money(this.amount.negate(), this.currency);
	}

	public boolean isPositive() {
		return this.amount.signum() > 0;
	}

	public boolean isNegative() {
		return this.amount.signum() < 0;
	}

	public boolean isZero() {
		return this.amount.signum() == 0;
	}

	public Money requirePositive() {
		if (this.amount.signum() <= 0) {
			throw new InvalidAmountException("Amount must be positive");
		}
		return this;
	}

	private void requireSameCurrency(Money other) {
		Objects.requireNonNull(other, "other");
		if (this.currency != other.currency) {
			throw new CurrencyMismatchException("Currency mismatch: " + this.currency + " vs " + other.currency);
		}
	}

	private static BigDecimal normalize(BigDecimal value) {
		return value.setScale(SCALE, RoundingMode.HALF_UP);
	}
}
