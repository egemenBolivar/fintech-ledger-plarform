package com.ekup.fintech.ledger.domain;

import java.util.Objects;
import java.util.UUID;

import com.ekup.fintech.auth.domain.User;
import com.ekup.fintech.shared.domain.BaseEntity;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.exception.WalletClosedException;
import com.ekup.fintech.shared.util.IdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "wallets",
		uniqueConstraints = @UniqueConstraint(name = "ux_wallet_owner_currency", columnNames = { "owner_id", "base_currency" })
)
public class Wallet extends BaseEntity {
	@Id
	@Column(nullable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	@Enumerated(EnumType.STRING)
	@Column(name = "base_currency", nullable = false, length = 3)
	private Currency baseCurrency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WalletStatus status;

	@Version
	private Long version = 0L;

	protected Wallet() {
	}

	private Wallet(UUID id, User owner, Currency baseCurrency, WalletStatus status) {
		this.id = Objects.requireNonNull(id, "id");
		this.owner = Objects.requireNonNull(owner, "owner");
		this.baseCurrency = Objects.requireNonNull(baseCurrency, "baseCurrency");
		this.status = Objects.requireNonNull(status, "status");
	}

	public static Wallet create(User owner, Currency baseCurrency) {
		return new Wallet(IdGenerator.newId(), owner, baseCurrency, WalletStatus.ACTIVE);
	}

	public UUID getId() {
		return id;
	}

	public UUID getOwnerId() {
		return owner != null ? owner.getId() : null;
	}

	public User getOwner() {
		return owner;
	}

	public Currency getBaseCurrency() {
		return baseCurrency;
	}

	public WalletStatus getStatus() {
		return status;
	}

	public Long getVersion() {
		return version;
	}

	public void suspend() {
		ensureNotClosed();
		this.status = WalletStatus.SUSPENDED;
	}

	public void activate() {
		ensureNotClosed();
		this.status = WalletStatus.ACTIVE;
	}

	public void close() {
		this.status = WalletStatus.CLOSED;
	}

	private void ensureNotClosed() {
		if (this.status == WalletStatus.CLOSED) {
			throw new WalletClosedException("Wallet is closed");
		}
	}
}
