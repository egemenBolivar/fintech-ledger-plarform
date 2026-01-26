package com.ekup.fintech.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;
import com.ekup.fintech.shared.exception.InvalidAmountException;
import com.ekup.fintech.shared.util.IdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "transactions")
public class Transaction {
	@Id
	@Column(nullable = false)
	private UUID id;

	@Column(name = "wallet_id", nullable = false)
	private UUID walletId;

	@Column(nullable = false, precision = 19, scale = Money.SCALE)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 3)
	private Currency currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TransactionDirection direction;

	@Enumerated(EnumType.STRING)
	@Column(name = "group_type", nullable = false, length = 30)
	private TransactionGroupType groupType;

	@Enumerated(EnumType.STRING)
	@Column(name = "reference_type", nullable = false, length = 30)
	private ReferenceType referenceType;

	@Column(name = "reference_id")
	private UUID referenceId;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	protected Transaction() {
	}

	private Transaction(
			UUID id,
			UUID walletId,
			BigDecimal amount,
			Currency currency,
			TransactionDirection direction,
			TransactionGroupType groupType,
			ReferenceType referenceType,
			UUID referenceId,
			String description,
			Instant occurredAt
	) {
		this.id = Objects.requireNonNull(id, "id");
		this.walletId = Objects.requireNonNull(walletId, "walletId");
		this.currency = Objects.requireNonNull(currency, "currency");
		this.direction = Objects.requireNonNull(direction, "direction");
		this.groupType = Objects.requireNonNull(groupType, "groupType");
		this.referenceType = Objects.requireNonNull(referenceType, "referenceType");
		this.referenceId = referenceId;
		this.description = description;
		this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");

		Money.of(Objects.requireNonNull(amount, "amount"), currency).requirePositive();
		this.amount = Money.of(amount, currency).amount();
	}

	public static Transaction credit(
			UUID walletId,
			Money amount,
			TransactionGroupType groupType,
			ReferenceType referenceType,
			UUID referenceId,
			String description,
			Instant occurredAt
	) {
		return create(walletId, amount, TransactionDirection.CREDIT, groupType, referenceType, referenceId, description, occurredAt);
	}

	public static Transaction debit(
			UUID walletId,
			Money amount,
			TransactionGroupType groupType,
			ReferenceType referenceType,
			UUID referenceId,
			String description,
			Instant occurredAt
	) {
		return create(walletId, amount, TransactionDirection.DEBIT, groupType, referenceType, referenceId, description, occurredAt);
	}

	private static Transaction create(
			UUID walletId,
			Money amount,
			TransactionDirection direction,
			TransactionGroupType groupType,
			ReferenceType referenceType,
			UUID referenceId,
			String description,
			Instant occurredAt
	) {
		Objects.requireNonNull(amount, "amount");
		amount.requirePositive();
		if (walletId == null) {
			throw new InvalidAmountException("walletId is required");
		}
		return new Transaction(
				IdGenerator.newId(),
				walletId,
				amount.amount(),
				amount.currency(),
				direction,
				groupType,
				referenceType,
				referenceId,
				description,
				occurredAt != null ? occurredAt : Instant.now()
		);
	}

	public UUID getId() {
		return id;
	}

	public UUID getWalletId() {
		return walletId;
	}

	public Money getMoney() {
		return Money.of(amount, currency);
	}

	public TransactionDirection getDirection() {
		return direction;
	}

	public TransactionGroupType getGroupType() {
		return groupType;
	}

	public ReferenceType getReferenceType() {
		return referenceType;
	}

	public UUID getReferenceId() {
		return referenceId;
	}

	public String getDescription() {
		return description;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}
}
