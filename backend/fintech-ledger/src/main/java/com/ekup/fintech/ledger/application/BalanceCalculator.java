package com.ekup.fintech.ledger.application;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ekup.fintech.ledger.infrastructure.persistence.TransactionJpaRepository;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;

@Service
public class BalanceCalculator {
	private final TransactionJpaRepository transactionRepository;
	private final BalanceSnapshotService snapshotService;
	private final boolean useSnapshot;

	public BalanceCalculator(
			TransactionJpaRepository transactionRepository,
			BalanceSnapshotService snapshotService,
			@Value("${fintech.balance.use-snapshot:true}") boolean useSnapshot) {
		this.transactionRepository = transactionRepository;
		this.snapshotService = snapshotService;
		this.useSnapshot = useSnapshot;
	}

	public Money calculateBalance(UUID walletId, Currency currency) {
		if (useSnapshot) {
			return snapshotService.calculateBalanceWithSnapshot(walletId, currency);
		}
		// Fallback: direct SUM
		BigDecimal signed = transactionRepository.sumSignedAmount(walletId, currency);
		return Money.of(signed, currency);
	}
	
	/**
	 * İşlem sonrası snapshot kontrolü için çağrılır
	 */
	public void checkAndCreateSnapshot(UUID walletId) {
		if (useSnapshot) {
			snapshotService.createSnapshotIfNeeded(walletId);
		}
	}
}
