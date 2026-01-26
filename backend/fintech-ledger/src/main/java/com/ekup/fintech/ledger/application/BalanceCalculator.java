package com.ekup.fintech.ledger.application;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ekup.fintech.ledger.infrastructure.persistence.TransactionJpaRepository;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;

@Service
public class BalanceCalculator {
	private final TransactionJpaRepository transactionRepository;

	public BalanceCalculator(TransactionJpaRepository transactionRepository) {
		this.transactionRepository = transactionRepository;
	}

	public Money calculateBalance(UUID walletId, Currency currency) {
		BigDecimal signed = transactionRepository.sumSignedAmount(walletId, currency);
		return Money.of(signed, currency);
	}
}
