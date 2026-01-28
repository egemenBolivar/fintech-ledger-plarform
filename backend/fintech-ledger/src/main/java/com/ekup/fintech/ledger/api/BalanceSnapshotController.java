package com.ekup.fintech.ledger.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ekup.fintech.ledger.application.BalanceSnapshotService;
import com.ekup.fintech.ledger.domain.BalanceSnapshot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/snapshots")
@Tag(name = "Admin - Balance Snapshots", description = "Balance snapshot management operations")
public class BalanceSnapshotController {
    
    private final BalanceSnapshotService snapshotService;
    
    public BalanceSnapshotController(BalanceSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }
    
    @PostMapping("/wallets/{walletId}")
    @Operation(summary = "Create snapshot for a specific wallet")
    public ResponseEntity<SnapshotResponse> createSnapshotForWallet(@PathVariable UUID walletId) {
        BalanceSnapshot snapshot = snapshotService.createSnapshot(walletId);
        return ResponseEntity.ok(SnapshotResponse.from(snapshot));
    }
    
    @PostMapping("/all")
    @Operation(summary = "Trigger snapshot creation for all wallets")
    public ResponseEntity<String> createSnapshotsForAll() {
        snapshotService.createSnapshotsForAllWallets();
        return ResponseEntity.ok("Snapshot creation triggered for all wallets");
    }
    
    public record SnapshotResponse(
        UUID id,
        UUID walletId,
        String balance,
        String currency,
        String snapshotAt,
        Long transactionCount
    ) {
        public static SnapshotResponse from(BalanceSnapshot snapshot) {
            return new SnapshotResponse(
                snapshot.getId(),
                snapshot.getWalletId(),
                snapshot.getBalance().amount().toPlainString(),
                snapshot.getCurrency().name(),
                snapshot.getSnapshotAt().toString(),
                snapshot.getTransactionCount()
            );
        }
    }
}
