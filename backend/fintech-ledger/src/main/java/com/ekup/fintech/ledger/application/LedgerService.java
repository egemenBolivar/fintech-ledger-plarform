package com.ekup.fintech.ledger.application;

import java.time.Instant;
import java.util.Objects;
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
import com.ekup.fintech.shared.exception.CurrencyMismatchException;
import com.ekup.fintech.shared.exception.InsufficientBalanceException;
import com.ekup.fintech.shared.exception.ResourceNotFoundException;
import com.ekup.fintech.shared.exception.SameWalletTransferException;
import com.ekup.fintech.shared.exception.WalletClosedException;
import com.ekup.fintech.shared.exception.WalletSuspendedException;

@Service
public class LedgerService {
	public record TransferResult(Transaction sourceTransaction, Transaction targetTransaction) {
	}

	private final WalletJpaRepository walletRepository;
	private final TransactionJpaRepository transactionRepository;
	private final BalanceCalculator balanceCalculator;

	public LedgerService(
			WalletJpaRepository walletRepository,
			TransactionJpaRepository transactionRepository,
			BalanceCalculator balanceCalculator
	) {
		this.walletRepository = walletRepository;
		this.transactionRepository = transactionRepository;
		this.balanceCalculator = balanceCalculator;
	}

	@Transactional
	public Transaction deposit(UUID walletId, Money amount, UUID depositId, String description) {
		Wallet wallet = getWalletRequired(walletId);
		ensureWalletOperational(wallet);
		requireWalletCurrency(wallet, amount);

		Transaction tx = Transaction.credit(
				wallet.getId(),
				amount,
				TransactionGroupType.USER_ACTION,
				ReferenceType.DEPOSIT,
				depositId,
				description,
				Instant.now()
		);
		Transaction saved = transactionRepository.save(tx);
		
		// Snapshot kontrolü (async olarak yapılabilir)
		balanceCalculator.checkAndCreateSnapshot(walletId);
		
		return saved;
	}

	@Transactional
	public Transaction withdraw(UUID walletId, Money amount, UUID withdrawalId, String description) {
		Wallet wallet = getWalletRequired(walletId);
		ensureWalletOperational(wallet);
		requireWalletCurrency(wallet, amount);
		ensureSufficientBalance(wallet, amount);

		Transaction tx = Transaction.debit(
				wallet.getId(),
				amount,
				TransactionGroupType.USER_ACTION,
				ReferenceType.WITHDRAWAL,
				withdrawalId,
				description,
				Instant.now()
		);
		Transaction saved = transactionRepository.save(tx);
		
		// Snapshot kontrolü
		balanceCalculator.checkAndCreateSnapshot(walletId);
		
		return saved;
	}

	@Transactional
	public TransferResult transfer(UUID sourceWalletId, UUID targetWalletId, Money amount, UUID transferId, String description) {
		Objects.requireNonNull(sourceWalletId, "sourceWalletId");
		Objects.requireNonNull(targetWalletId, "targetWalletId");
		Objects.requireNonNull(amount, "amount");

		if (sourceWalletId.equals(targetWalletId)) {
			throw new SameWalletTransferException("Cannot transfer to the same wallet");
		}

		Wallet source = getWalletRequired(sourceWalletId);
		Wallet target = getWalletRequired(targetWalletId);

		ensureWalletOperational(source);
		ensureWalletOperational(target);
		requireWalletCurrency(source, amount);
		requireWalletCurrency(target, amount);
		ensureSufficientBalance(source, amount);

		Transaction debit = Transaction.debit(
				source.getId(),
				amount,
				TransactionGroupType.USER_ACTION,
				ReferenceType.TRANSFER,
				transferId,
				description,
				Instant.now()
		);
		Transaction credit = Transaction.credit(
				target.getId(),
				amount,
				TransactionGroupType.USER_ACTION,
				ReferenceType.TRANSFER,
				transferId,
				description,
				Instant.now()
		);

		Transaction savedDebit = transactionRepository.save(debit);
		Transaction savedCredit = transactionRepository.save(credit);
		
		// Snapshot kontrolü (her iki wallet için)
		balanceCalculator.checkAndCreateSnapshot(sourceWalletId);
		balanceCalculator.checkAndCreateSnapshot(targetWalletId);
		
		return new TransferResult(savedDebit, savedCredit);
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

	private void requireWalletCurrency(Wallet wallet, Money amount) {
		if (wallet.getBaseCurrency() != amount.currency()) {
			throw new CurrencyMismatchException("Wallet currency mismatch: " + wallet.getBaseCurrency() + " vs " + amount.currency());
		}
	}

	private void ensureSufficientBalance(Wallet wallet, Money debitAmount) {
		Money balance = balanceCalculator.calculateBalance(wallet.getId(), debitAmount.currency());
		if (balance.amount().compareTo(debitAmount.amount()) < 0) {
			throw new InsufficientBalanceException("Insufficient balance");
		}
	}
}
