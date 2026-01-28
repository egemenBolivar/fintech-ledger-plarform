package com.ekup.fintech.ledger.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ekup.fintech.ledger.domain.BalanceSnapshot;
import com.ekup.fintech.shared.domain.Currency;

public interface BalanceSnapshotJpaRepository extends JpaRepository<BalanceSnapshot, UUID> {
    
    /**
     * En son snapshot'ı getir
     */
    Optional<BalanceSnapshot> findTopByWalletIdOrderBySnapshotAtDesc(UUID walletId);
    
    /**
     * Belirli bir tarihten sonraki işlemlerin signed toplamını hesapla
     */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN t.direction = com.ekup.fintech.ledger.domain.TransactionDirection.CREDIT THEN t.amount ELSE -t.amount END), 0) " +
        "FROM Transaction t " +
        "WHERE t.walletId = :walletId AND t.currency = :currency AND t.occurredAt > :after"
    )
    BigDecimal sumSignedAmountAfter(
        @Param("walletId") UUID walletId, 
        @Param("currency") Currency currency, 
        @Param("after") Instant after
    );
    
    /**
     * Belirli bir tarihten sonraki işlem sayısını getir
     */
    @Query(
        "SELECT COUNT(t) FROM Transaction t " +
        "WHERE t.walletId = :walletId AND t.occurredAt > :after"
    )
    Long countTransactionsAfter(@Param("walletId") UUID walletId, @Param("after") Instant after);
    
    /**
     * Wallet için toplam işlem sayısı
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.walletId = :walletId")
    Long countTransactionsByWalletId(@Param("walletId") UUID walletId);
    
    /**
     * En son işlem ID'sini getir
     */
    @Query(
        "SELECT t.id FROM Transaction t " +
        "WHERE t.walletId = :walletId " +
        "ORDER BY t.occurredAt DESC " +
        "LIMIT 1"
    )
    Optional<UUID> findLastTransactionId(@Param("walletId") UUID walletId);
    
    /**
     * Eski snapshot'ları temizle (son N tane hariç)
     */
    @Query(
        "DELETE FROM BalanceSnapshot bs " +
        "WHERE bs.walletId = :walletId " +
        "AND bs.id NOT IN (" +
        "  SELECT bs2.id FROM BalanceSnapshot bs2 " +
        "  WHERE bs2.walletId = :walletId " +
        "  ORDER BY bs2.snapshotAt DESC " +
        "  LIMIT :keepCount" +
        ")"
    )
    void deleteOldSnapshots(@Param("walletId") UUID walletId, @Param("keepCount") int keepCount);
}
