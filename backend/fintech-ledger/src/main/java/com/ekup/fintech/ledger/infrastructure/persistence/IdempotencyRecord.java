package com.ekup.fintech.ledger.infrastructure.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "idempotency_records",
		uniqueConstraints = @UniqueConstraint(
				name = "ux_idem_operation_key",
				columnNames = { "operation", "idempotency_key" }
		)
)
public class IdempotencyRecord {
	@Id
	@Column(nullable = false)
	private UUID id;

	@Column(nullable = false, length = 30)
	private String operation;

	@Column(name = "idempotency_key", nullable = false)
	private UUID idempotencyKey;

	@Column(name = "request_fingerprint", nullable = false, length = 300)
	private String requestFingerprint;

	@Column(name = "wallet_id")
	private UUID walletId;

	@Column(name = "transaction_id")
	private UUID transactionId;

	@Column(name = "transaction_id_2")
	private UUID transactionId2;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected IdempotencyRecord() {
	}

	public IdempotencyRecord(
			UUID id,
			String operation,
			UUID idempotencyKey,
			String requestFingerprint,
			UUID walletId,
			UUID transactionId,
			UUID transactionId2
	) {
		this.id = Objects.requireNonNull(id, "id");
		this.operation = Objects.requireNonNull(operation, "operation");
		this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
		this.requestFingerprint = Objects.requireNonNull(requestFingerprint, "requestFingerprint");
		this.walletId = walletId;
		this.transactionId = transactionId;
		this.transactionId2 = transactionId2;
	}

	@PrePersist
	void onCreate() {
		if (this.createdAt == null) {
			this.createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public String getOperation() {
		return operation;
	}

	public UUID getIdempotencyKey() {
		return idempotencyKey;
	}

	public String getRequestFingerprint() {
		return requestFingerprint;
	}

	public UUID getWalletId() {
		return walletId;
	}

	public UUID getTransactionId() {
		return transactionId;
	}

	public UUID getTransactionId2() {
		return transactionId2;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
