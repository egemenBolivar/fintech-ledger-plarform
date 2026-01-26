package com.ekup.fintech.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.ekup.fintech.shared.exception.CurrencyMismatchException;
import com.ekup.fintech.shared.exception.InvalidAmountException;

class MoneyTest {
	@Test
	void normalizesScaleTo4() {
		Money m = Money.of(new BigDecimal("10"), Currency.USD);
		assertThat(m.amount().scale()).isEqualTo(4);
		assertThat(m.amount()).isEqualByComparingTo(new BigDecimal("10.0000"));
	}

	@Test
	void addRejectsCurrencyMismatch() {
		Money usd = Money.of(new BigDecimal("1"), Currency.USD);
		Money eur = Money.of(new BigDecimal("1"), Currency.EUR);
		assertThatThrownBy(() -> usd.add(eur)).isInstanceOf(CurrencyMismatchException.class);
	}

	@Test
	void requirePositiveRejectsZeroOrNegative() {
		assertThatThrownBy(() -> Money.of(new BigDecimal("0"), Currency.TRY).requirePositive())
				.isInstanceOf(InvalidAmountException.class);
		assertThatThrownBy(() -> Money.of(new BigDecimal("-1"), Currency.TRY).requirePositive())
				.isInstanceOf(InvalidAmountException.class);
	}
}
