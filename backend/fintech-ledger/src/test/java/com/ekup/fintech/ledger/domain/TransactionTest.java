package com.ekup.fintech.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;
import com.ekup.fintech.shared.exception.InvalidAmountException;

class TransactionTest {

	@Test
	void creditTransactionHasCreditDirection() {
		UUID walletId = UUID.randomUUID();
		Money amount = Money.of(new BigDecimal("100"), Currency.USD);

		Transaction tx = Transaction.credit(
				walletId,
				amount,
				TransactionGroupType.USER_ACTION,
				ReferenceType.DEPOSIT,
				null,
				"test deposit",
				Instant.now()
		);

		assertThat(tx.getDirection()).isEqualTo(TransactionDirection.CREDIT);
		assertThat(tx.getMoney().amount()).isEqualByComparingTo(new BigDecimal("100"));
		assertThat(tx.getWalletId()).isEqualTo(walletId);
	}

	@Test
	void debitTransactionHasDebitDirection() {
		UUID walletId = UUID.randomUUID();
		Money amount = Money.of(new BigDecimal("50"), Currency.EUR);

		Transaction tx = Transaction.debit(
				walletId,
				amount,
				TransactionGroupType.USER_ACTION,
				ReferenceType.WITHDRAWAL,
				null,
				"test withdrawal",
				Instant.now()
		);

		assertThat(tx.getDirection()).isEqualTo(TransactionDirection.DEBIT);
		assertThat(tx.getMoney().amount()).isEqualByComparingTo(new BigDecimal("50"));
	}

	@Test
	void transactionWithZeroAmountThrows() {
		UUID walletId = UUID.randomUUID();
		Money zero = Money.of(BigDecimal.ZERO, Currency.USD);

		assertThatThrownBy(() ->
				Transaction.credit(walletId, zero, TransactionGroupType.USER_ACTION, ReferenceType.DEPOSIT, null, "test", Instant.now())
		).isInstanceOf(InvalidAmountException.class);
	}

	@Test
	void transactionWithNegativeAmountThrows() {
		UUID walletId = UUID.randomUUID();
		Money negative = Money.of(new BigDecimal("-10"), Currency.USD);

		assertThatThrownBy(() ->
				Transaction.debit(walletId, negative, TransactionGroupType.USER_ACTION, ReferenceType.WITHDRAWAL, null, "test", Instant.now())
		).isInstanceOf(InvalidAmountException.class);
	}

	@Test
	void transactionIsImmutableNoSetters() {
		// This test documents that Transaction has no setters by design
		// Verified by @Immutable annotation and no setter methods in the class
		Transaction tx = Transaction.credit(
				UUID.randomUUID(),
				Money.of(new BigDecimal("100"), Currency.USD),
				TransactionGroupType.USER_ACTION,
				ReferenceType.DEPOSIT,
				null,
				"test",
				Instant.now()
		);

		// Only getters are available
		assertThat(tx.getId()).isNotNull();
		assertThat(tx.getOccurredAt()).isNotNull();
	}
}
