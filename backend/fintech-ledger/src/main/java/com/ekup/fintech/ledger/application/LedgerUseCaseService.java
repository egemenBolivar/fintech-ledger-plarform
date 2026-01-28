package com.ekup.fintech.ledger.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ekup.fintech.ledger.domain.Transaction;
import com.ekup.fintech.ledger.infrastructure.persistence.IdempotencyRecord;
import com.ekup.fintech.ledger.infrastructure.persistence.IdempotencyRecordJpaRepository;
import com.ekup.fintech.ledger.infrastructure.persistence.TransactionJpaRepository;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;
import com.ekup.fintech.shared.exception.IdempotencyConflictException;
import com.ekup.fintech.shared.exception.ResourceNotFoundException;
import com.ekup.fintech.shared.util.IdGenerator;

@Service
public class LedgerUseCaseService {
	public record SingleTransactionResult(UUID requestId, Transaction transaction, Instant processedAt) {
	}

	public record TransferTransactionsResult(UUID requestId, Transaction sourceTransaction, Transaction targetTransaction, Instant processedAt) {
	}

	private static final String OP_DEPOSIT = "DEPOSIT";
	private static final String OP_WITHDRAWAL = "WITHDRAWAL";
	private static final String OP_TRANSFER = "TRANSFER";

	private final LedgerService ledgerService;
	private final TransactionJpaRepository transactionRepository;
	private final IdempotencyRecordJpaRepository idempotencyRepository;

	public LedgerUseCaseService(
			LedgerService ledgerService,
			TransactionJpaRepository transactionRepository,
			IdempotencyRecordJpaRepository idempotencyRepository
	) {
		this.ledgerService = ledgerService;
		this.transactionRepository = transactionRepository;
		this.idempotencyRepository = idempotencyRepository;
	}

	@Transactional
	public SingleTransactionResult deposit(UUID walletId, Money amount, UUID idempotencyKey, String description) {
		UUID key = keyOrNew(idempotencyKey);
		String fingerprint = fingerprintSingle(walletId, amount.amount(), amount.currency());

		IdempotencyRecord cached = idempotencyRepository.findByOperationAndIdempotencyKey(OP_DEPOSIT, key).orElse(null);
		if (cached != null) {
			ensureSameRequest(OP_DEPOSIT, key, cached.getRequestFingerprint(), fingerprint);
			Transaction tx = getTransactionRequired(cached.getTransactionId());
			return new SingleTransactionResult(key, tx, cached.getCreatedAt());
		}

		Transaction created = ledgerService.deposit(walletId, amount, key, description);
		trySaveIdempotency(new IdempotencyRecord(
				IdGenerator.newId(),
				OP_DEPOSIT,
				key,
				fingerprint,
				walletId,
				created.getId(),
				null
		));
		return new SingleTransactionResult(key, created, Instant.now());
	}

	@Transactional
	public SingleTransactionResult withdraw(UUID walletId, Money amount, UUID idempotencyKey, String description) {
		UUID key = keyOrNew(idempotencyKey);
		String fingerprint = fingerprintSingle(walletId, amount.amount(), amount.currency());

		IdempotencyRecord cached = idempotencyRepository.findByOperationAndIdempotencyKey(OP_WITHDRAWAL, key).orElse(null);
		if (cached != null) {
			ensureSameRequest(OP_WITHDRAWAL, key, cached.getRequestFingerprint(), fingerprint);
			Transaction tx = getTransactionRequired(cached.getTransactionId());
			return new SingleTransactionResult(key, tx, cached.getCreatedAt());
		}

		Transaction created = ledgerService.withdraw(walletId, amount, key, description);
		trySaveIdempotency(new IdempotencyRecord(
				IdGenerator.newId(),
				OP_WITHDRAWAL,
				key,
				fingerprint,
				walletId,
				created.getId(),
				null
		));
		return new SingleTransactionResult(key, created, Instant.now());
	}

	@Transactional
	public TransferTransactionsResult transfer(
			UUID sourceWalletId,
			UUID targetWalletId,
			Money amount,
			UUID idempotencyKey,
			String description
	) {
		UUID key = keyOrNew(idempotencyKey);
		String fingerprint = fingerprintTransfer(sourceWalletId, targetWalletId, amount.amount(), amount.currency(), description);

		IdempotencyRecord cached = idempotencyRepository.findByOperationAndIdempotencyKey(OP_TRANSFER, key).orElse(null);
		if (cached != null) {
			ensureSameRequest(OP_TRANSFER, key, cached.getRequestFingerprint(), fingerprint);
			Transaction sourceTx = getTransactionRequired(cached.getTransactionId());
			Transaction targetTx = getTransactionRequired(cached.getTransactionId2());
			return new TransferTransactionsResult(key, sourceTx, targetTx, cached.getCreatedAt());
		}

		LedgerService.TransferResult result = ledgerService.transfer(sourceWalletId, targetWalletId, amount, key, description);
		trySaveIdempotency(new IdempotencyRecord(
				IdGenerator.newId(),
				OP_TRANSFER,
				key,
				fingerprint,
				sourceWalletId,
				result.sourceTransaction().getId(),
				result.targetTransaction().getId()
		));

		return new TransferTransactionsResult(key, result.sourceTransaction(), result.targetTransaction(), Instant.now());
	}

	private Transaction getTransactionRequired(UUID id) {
		if (id == null) {
			throw new ResourceNotFoundException("Transaction id is missing for idempotency record");
		}
		return transactionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
	}

	private void ensureSameRequest(String operation, UUID key, String existingFingerprint, String incomingFingerprint) {
		if (!Objects.equals(existingFingerprint, incomingFingerprint)) {
			throw new IdempotencyConflictException(
					"Idempotency key reuse with different payload for operation=" + operation + ", key=" + key
			);
		}
	}

	private void trySaveIdempotency(IdempotencyRecord record) {
		try {
			idempotencyRepository.save(record);
		} catch (DataIntegrityViolationException e) {
			// Another request with same (operation,key) won the race. OK: we will serve cached next time.
			idempotencyRepository.findByOperationAndIdempotencyKey(record.getOperation(), record.getIdempotencyKey())
					.ifPresent(existing -> ensureSameRequest(record.getOperation(), record.getIdempotencyKey(), existing.getRequestFingerprint(), record.getRequestFingerprint()));
		}
	}

	private static UUID keyOrNew(UUID idempotencyKey) {
		return idempotencyKey != null ? idempotencyKey : IdGenerator.newId();
	}

	private static String fingerprintSingle(UUID walletId, java.math.BigDecimal amount, Currency currency) {
		return "walletId=" + walletId + "|amount=" + amount + "|currency=" + currency;
	}

	private static String fingerprintTransfer(UUID sourceWalletId, UUID targetWalletId, java.math.BigDecimal amount, Currency currency, String description) {
		return "sourceWalletId=" + sourceWalletId + "|targetWalletId=" + targetWalletId + "|amount=" + amount + "|currency=" + currency + "|description=" + (description != null ? description : "");
	}
}
