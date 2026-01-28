package com.ekup.fintech.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;
import com.ekup.fintech.shared.util.IdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Balance Snapshot - Periyodik olarak alınan bakiye anlık görüntüsü.
 * Bu sayede her seferinde tüm işlemlerin SUM'u alınmak yerine,
 * son snapshot'tan sonraki işlemler hesaplanarak performans artırılır.
 */
@Entity
@Table(
    name = "balance_snapshots",
    indexes = {
        @Index(name = "idx_snapshot_wallet_timestamp", columnList = "wallet_id, snapshot_at DESC")
    }
)
public class BalanceSnapshot {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(nullable = false, precision = 19, scale = Money.SCALE)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    @Column(name = "transaction_count", nullable = false)
    private Long transactionCount;

    @Column(name = "last_transaction_id")
    private UUID lastTransactionId;

    protected BalanceSnapshot() {
    }

    private BalanceSnapshot(UUID id, UUID walletId, BigDecimal balance, Currency currency, 
                            Instant snapshotAt, Long transactionCount, UUID lastTransactionId) {
        this.id = Objects.requireNonNull(id, "id");
        this.walletId = Objects.requireNonNull(walletId, "walletId");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.snapshotAt = Objects.requireNonNull(snapshotAt, "snapshotAt");
        this.transactionCount = Objects.requireNonNull(transactionCount, "transactionCount");
        this.lastTransactionId = lastTransactionId; // can be null if no transactions
    }

    public static BalanceSnapshot create(UUID walletId, Money balance, Long transactionCount, UUID lastTransactionId) {
        return new BalanceSnapshot(
            IdGenerator.newId(),
            walletId,
            balance.amount(),
            balance.currency(),
            Instant.now(),
            transactionCount,
            lastTransactionId
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public Money getBalance() {
        return Money.of(balance, currency);
    }

    public Currency getCurrency() {
        return currency;
    }

    public Instant getSnapshotAt() {
        return snapshotAt;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public UUID getLastTransactionId() {
        return lastTransactionId;
    }
}
