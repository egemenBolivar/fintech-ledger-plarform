package com.ekup.fintech.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ekup.fintech.ledger.domain.ReferenceType;
import com.ekup.fintech.ledger.domain.Transaction;
import com.ekup.fintech.ledger.domain.TransactionGroupType;
import com.ekup.fintech.ledger.domain.Wallet;
import com.ekup.fintech.ledger.infrastructure.persistence.TransactionJpaRepository;
import com.ekup.fintech.ledger.infrastructure.persistence.WalletJpaRepository;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;

@SpringBootTest
@Transactional
class BalanceCalculatorJpaTest {
	@Autowired
	WalletJpaRepository walletRepository;

	@Autowired
	TransactionJpaRepository transactionRepository;

	@Autowired
	BalanceCalculator balanceCalculator;

	@Test
	void calculatesSignedBalanceFromImmutableTransactions() {
		Wallet wallet = walletRepository.save(Wallet.create(java.util.UUID.randomUUID(), Currency.USD));

		transactionRepository.save(Transaction.credit(
				wallet.getId(),
				Money.of(new BigDecimal("100"), Currency.USD),
				TransactionGroupType.USER_ACTION,
				ReferenceType.DEPOSIT,
				null,
				"deposit",
				Instant.now()
		));
		transactionRepository.save(Transaction.debit(
				wallet.getId(),
				Money.of(new BigDecimal("30"), Currency.USD),
				TransactionGroupType.USER_ACTION,
				ReferenceType.WITHDRAWAL,
				null,
				"withdraw",
				Instant.now()
		));

		Money balance = balanceCalculator.calculateBalance(wallet.getId(), Currency.USD);
		assertThat(balance.amount()).isEqualByComparingTo(new BigDecimal("70.0000"));
		assertThat(balance.currency()).isEqualTo(Currency.USD);
	}
}
