package com.ekup.fintech.ledger.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ekup.fintech.ledger.application.BalanceSnapshotService;

/**
 * Scheduled job for periodic balance snapshot creation.
 * Runs every hour to ensure all wallets have up-to-date snapshots.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "fintech.balance.use-snapshot", havingValue = "true", matchIfMissing = true)
public class BalanceSnapshotScheduler {
    private static final Logger log = LoggerFactory.getLogger(BalanceSnapshotScheduler.class);
    
    private final BalanceSnapshotService snapshotService;
    
    public BalanceSnapshotScheduler(BalanceSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }
    
    /**
     * Her saat başı snapshot kontrolü yap
     * Cron: saniye dakika saat gün ay haftanınGünü
     */
    @Scheduled(cron = "0 0 * * * *")
    public void hourlySnapshotJob() {
        log.info("Starting hourly balance snapshot job");
        try {
            snapshotService.createSnapshotsForAllWallets();
            log.info("Hourly balance snapshot job completed successfully");
        } catch (Exception e) {
            log.error("Hourly balance snapshot job failed", e);
        }
    }
}
