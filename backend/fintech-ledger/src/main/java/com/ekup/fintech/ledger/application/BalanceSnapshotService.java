package com.ekup.fintech.ledger.application;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ekup.fintech.ledger.domain.BalanceSnapshot;
import com.ekup.fintech.ledger.domain.Wallet;
import com.ekup.fintech.ledger.infrastructure.persistence.BalanceSnapshotJpaRepository;
import com.ekup.fintech.ledger.infrastructure.persistence.TransactionJpaRepository;
import com.ekup.fintech.ledger.infrastructure.persistence.WalletJpaRepository;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;

/**
 * Balance Snapshot Service - Snapshot tabanlı hızlı bakiye hesaplama.
 * 
 * Strateji:
 * 1. Son snapshot varsa: snapshot balance + snapshot'tan sonraki işlemler
 * 2. Son snapshot yoksa: tüm işlemlerin SUM'u (fallback)
 * 
 * Snapshot'lar belirli eşiklerde otomatik oluşturulur:
 * - Her 100 işlemde bir
 * - Manuel trigger ile
 */
@Service
public class BalanceSnapshotService {
    private static final Logger log = LoggerFactory.getLogger(BalanceSnapshotService.class);
    
    // Kaç işlemde bir snapshot alınacak
    private static final long SNAPSHOT_THRESHOLD = 100;
    
    // Her wallet için max kaç snapshot tutulacak
    private static final int MAX_SNAPSHOTS_PER_WALLET = 5;
    
    private final BalanceSnapshotJpaRepository snapshotRepository;
    private final TransactionJpaRepository transactionRepository;
    private final WalletJpaRepository walletRepository;
    
    public BalanceSnapshotService(
            BalanceSnapshotJpaRepository snapshotRepository,
            TransactionJpaRepository transactionRepository,
            WalletJpaRepository walletRepository) {
        this.snapshotRepository = snapshotRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }
    
    /**
     * Snapshot tabanlı hızlı bakiye hesaplama
     */
    @Transactional(readOnly = true)
    public Money calculateBalanceWithSnapshot(UUID walletId, Currency currency) {
        Optional<BalanceSnapshot> latestSnapshot = snapshotRepository.findTopByWalletIdOrderBySnapshotAtDesc(walletId);
        
        if (latestSnapshot.isPresent()) {
            BalanceSnapshot snapshot = latestSnapshot.get();
            
            // Snapshot'tan sonraki işlemlerin toplamını al
            BigDecimal deltaAmount = snapshotRepository.sumSignedAmountAfter(
                walletId, currency, snapshot.getSnapshotAt()
            );
            
            // Snapshot balance + delta
            BigDecimal totalBalance = snapshot.getBalance().amount().add(deltaAmount);
            
            log.debug("Balance calculated with snapshot for wallet {}: {} + {} = {}", 
                walletId, snapshot.getBalance().amount(), deltaAmount, totalBalance);
            
            return Money.of(totalBalance, currency);
        }
        
        // Fallback: Tüm işlemlerin SUM'u
        log.debug("No snapshot found for wallet {}, using full SUM", walletId);
        BigDecimal signed = transactionRepository.sumSignedAmount(walletId, currency);
        return Money.of(signed, currency);
    }
    
    /**
     * Gerekirse otomatik snapshot oluştur
     */
    @Transactional
    public void createSnapshotIfNeeded(UUID walletId) {
        Optional<BalanceSnapshot> latestSnapshot = snapshotRepository.findTopByWalletIdOrderBySnapshotAtDesc(walletId);
        
        long transactionsSinceSnapshot;
        if (latestSnapshot.isPresent()) {
            transactionsSinceSnapshot = snapshotRepository.countTransactionsAfter(
                walletId, latestSnapshot.get().getSnapshotAt()
            );
        } else {
            transactionsSinceSnapshot = snapshotRepository.countTransactionsByWalletId(walletId);
        }
        
        if (transactionsSinceSnapshot >= SNAPSHOT_THRESHOLD) {
            log.info("Creating snapshot for wallet {} (transactions since last: {})", 
                walletId, transactionsSinceSnapshot);
            createSnapshot(walletId);
        }
    }
    
    /**
     * Manuel snapshot oluşturma
     */
    @Transactional
    public BalanceSnapshot createSnapshot(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        
        // Mevcut bakiyeyi hesapla (SUM ile)
        BigDecimal balance = transactionRepository.sumSignedAmount(walletId, wallet.getBaseCurrency());
        Money balanceMoney = Money.of(balance, wallet.getBaseCurrency());
        
        // İşlem sayısı ve son işlem ID
        Long transactionCount = snapshotRepository.countTransactionsByWalletId(walletId);
        UUID lastTransactionId = snapshotRepository.findLastTransactionId(walletId).orElse(null);
        
        // Snapshot oluştur
        BalanceSnapshot snapshot = BalanceSnapshot.create(walletId, balanceMoney, transactionCount, lastTransactionId);
        snapshotRepository.save(snapshot);
        
        log.info("Created snapshot for wallet {}: balance={}, txCount={}", 
            walletId, balanceMoney, transactionCount);
        
        // Eski snapshot'ları temizle (async yapılabilir)
        cleanupOldSnapshots(walletId);
        
        return snapshot;
    }
    
    /**
     * Tüm wallet'lar için snapshot oluştur (scheduled job için)
     */
    @Transactional
    public void createSnapshotsForAllWallets() {
        log.info("Starting batch snapshot creation for all wallets");
        
        walletRepository.findAll().forEach(wallet -> {
            try {
                createSnapshotIfNeeded(wallet.getId());
            } catch (Exception e) {
                log.error("Failed to create snapshot for wallet {}: {}", wallet.getId(), e.getMessage());
            }
        });
        
        log.info("Batch snapshot creation completed");
    }
    
    /**
     * Eski snapshot'ları temizle
     */
    private void cleanupOldSnapshots(UUID walletId) {
        try {
            snapshotRepository.deleteOldSnapshots(walletId, MAX_SNAPSHOTS_PER_WALLET);
        } catch (Exception e) {
            log.warn("Failed to cleanup old snapshots for wallet {}: {}", walletId, e.getMessage());
        }
    }
}
