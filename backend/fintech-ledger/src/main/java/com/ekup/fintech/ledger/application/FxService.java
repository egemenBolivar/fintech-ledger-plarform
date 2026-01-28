package com.ekup.fintech.ledger.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ekup.fintech.ledger.domain.ReferenceType;
import com.ekup.fintech.ledger.domain.Transaction;
import com.ekup.fintech.ledger.domain.TransactionGroupType;
import com.ekup.fintech.ledger.domain.Wallet;
import com.ekup.fintech.ledger.domain.WalletStatus;
import com.ekup.fintech.ledger.infrastructure.persistence.TransactionJpaRepository;
import com.ekup.fintech.ledger.infrastructure.persistence.WalletJpaRepository;
import com.ekup.fintech.shared.domain.Money;
import com.ekup.fintech.shared.exception.InsufficientBalanceException;
import com.ekup.fintech.shared.exception.ResourceNotFoundException;
import com.ekup.fintech.shared.exception.WalletClosedException;
import com.ekup.fintech.shared.exception.WalletSuspendedException;

@Service
public class FxService {
	public record FxConversionResult(
			Transaction debitTransaction,
			Transaction creditTransaction,
			BigDecimal exchangeRate,
			Money sourceAmount,
			Money targetAmount
	) {
	}

	private final WalletJpaRepository walletRepository;
	private final TransactionJpaRepository transactionRepository;
	private final BalanceCalculator balanceCalculator;
	private final FxRateProvider fxRateProvider;

	public FxService(
			WalletJpaRepository walletRepository,
			TransactionJpaRepository transactionRepository,
			BalanceCalculator balanceCalculator,
			FxRateProvider fxRateProvider
	) {
		this.walletRepository = walletRepository;
		this.transactionRepository = transactionRepository;
		this.balanceCalculator = balanceCalculator;
		this.fxRateProvider = fxRateProvider;
	}

	/**
	 * Convert currency within a single wallet's owner context.
	 * Debits sourceAmount from source currency, credits converted amount to target currency.
	 * Both wallets must belong to same owner.
	 */
	@Transactional
	public FxConversionResult convert(
			UUID sourceWalletId,
			UUID targetWalletId,
			Money sourceAmount,
			UUID conversionId,
			String description
	) {
		Wallet sourceWallet = getWalletRequired(sourceWalletId);
		Wallet targetWallet = getWalletRequired(targetWalletId);

		ensureWalletOperational(sourceWallet);
		ensureWalletOperational(targetWallet);

		// Validate source wallet currency matches source amount
		if (sourceWallet.getBaseCurrency() != sourceAmount.currency()) {
			throw new IllegalArgumentException("Source amount currency must match source wallet currency");
		}

		// Get exchange rate and calculate target amount
		BigDecimal rate = fxRateProvider.getRate(sourceAmount.currency(), targetWallet.getBaseCurrency());
		Money targetAmount = fxRateProvider.convert(sourceAmount, targetWallet.getBaseCurrency());

		// Check sufficient balance in source wallet
		ensureSufficientBalance(sourceWallet, sourceAmount);

		Instant now = Instant.now();
		String desc = description != null ? description : "FX conversion";

		// Debit source wallet
		Transaction debit = Transaction.debit(
				sourceWallet.getId(),
				sourceAmount,
				TransactionGroupType.FX_CONVERSION,
				ReferenceType.FX_EXCHANGE,
				conversionId,
				desc + " (from " + sourceAmount.currency() + " to " + targetWallet.getBaseCurrency() + ")",
				now
		);

		// Credit target wallet
		Transaction credit = Transaction.credit(
				targetWallet.getId(),
				targetAmount,
				TransactionGroupType.FX_CONVERSION,
				ReferenceType.FX_EXCHANGE,
				conversionId,
				desc + " (from " + sourceAmount.currency() + " to " + targetWallet.getBaseCurrency() + ")",
				now
		);

		Transaction savedDebit = transactionRepository.save(debit);
		Transaction savedCredit = transactionRepository.save(credit);

		return new FxConversionResult(savedDebit, savedCredit, rate, sourceAmount, targetAmount);
	}

	private Wallet getWalletRequired(UUID walletId) {
		return walletRepository.findById(walletId)
				.orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletId));
	}

	private void ensureWalletOperational(Wallet wallet) {
		if (wallet.getStatus() == WalletStatus.SUSPENDED) {
			throw new WalletSuspendedException("Wallet is suspended");
		}
		if (wallet.getStatus() == WalletStatus.CLOSED) {
			throw new WalletClosedException("Wallet is closed");
		}
	}

	private void ensureSufficientBalance(Wallet wallet, Money debitAmount) {
		Money balance = balanceCalculator.calculateBalance(wallet.getId(), debitAmount.currency());
		if (balance.amount().compareTo(debitAmount.amount()) < 0) {
			throw new InsufficientBalanceException("Insufficient balance for FX conversion");
		}
	}
}
