package com.ekup.fintech.ledger.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ekup.fintech.auth.domain.Role;
import com.ekup.fintech.auth.domain.User;
import com.ekup.fintech.ledger.domain.BalanceSnapshot;
import com.ekup.fintech.ledger.domain.Wallet;
import com.ekup.fintech.ledger.infrastructure.persistence.BalanceSnapshotJpaRepository;
import com.ekup.fintech.ledger.infrastructure.persistence.TransactionJpaRepository;
import com.ekup.fintech.ledger.infrastructure.persistence.WalletJpaRepository;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;

@ExtendWith(MockitoExtension.class)
class BalanceSnapshotServiceTest {

    @Mock
    private BalanceSnapshotJpaRepository snapshotRepository;

    @Mock
    private TransactionJpaRepository transactionRepository;

    @Mock
    private WalletJpaRepository walletRepository;

    @InjectMocks
    private BalanceSnapshotService snapshotService;

    private static final UUID WALLET_ID = UUID.randomUUID();
    private static final Currency CURRENCY = Currency.USD;

    private User createTestUser() {
        return User.create("test@example.com", "password", "Test User", Set.of(Role.USER));
    }

    @Nested
    @DisplayName("calculateBalanceWithSnapshot")
    class CalculateBalanceWithSnapshotTests {

        @Test
        @DisplayName("should calculate balance using snapshot + delta")
        void shouldCalculateBalanceWithSnapshotAndDelta() {
            // Given
            Money snapshotBalance = Money.of(BigDecimal.valueOf(1000), CURRENCY);
            BalanceSnapshot snapshot = BalanceSnapshot.create(WALLET_ID, snapshotBalance, 50L, UUID.randomUUID());
            
            when(snapshotRepository.findTopByWalletIdOrderBySnapshotAtDesc(WALLET_ID))
                .thenReturn(Optional.of(snapshot));
            when(snapshotRepository.sumSignedAmountAfter(eq(WALLET_ID), eq(CURRENCY), any(Instant.class)))
                .thenReturn(BigDecimal.valueOf(250)); // +250 after snapshot

            // When
            Money result = snapshotService.calculateBalanceWithSnapshot(WALLET_ID, CURRENCY);

            // Then
            assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(1250)); // 1000 + 250
            assertThat(result.currency()).isEqualTo(CURRENCY);
        }

        @Test
        @DisplayName("should fallback to SUM when no snapshot exists")
        void shouldFallbackToSumWhenNoSnapshot() {
            // Given
            when(snapshotRepository.findTopByWalletIdOrderBySnapshotAtDesc(WALLET_ID))
                .thenReturn(Optional.empty());
            when(transactionRepository.sumSignedAmount(WALLET_ID, CURRENCY))
                .thenReturn(BigDecimal.valueOf(500));

            // When
            Money result = snapshotService.calculateBalanceWithSnapshot(WALLET_ID, CURRENCY);

            // Then
            assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(500));
            verify(transactionRepository).sumSignedAmount(WALLET_ID, CURRENCY);
        }
    }

    @Nested
    @DisplayName("createSnapshotIfNeeded")
    class CreateSnapshotIfNeededTests {

        @Test
        @DisplayName("should create snapshot when threshold is reached")
        void shouldCreateSnapshotWhenThresholdReached() {
            // Given
            when(snapshotRepository.findTopByWalletIdOrderBySnapshotAtDesc(WALLET_ID))
                .thenReturn(Optional.empty());
            when(snapshotRepository.countTransactionsByWalletId(WALLET_ID))
                .thenReturn(100L); // >= threshold
            
            Wallet wallet = Wallet.create(createTestUser(), CURRENCY);
            when(walletRepository.findById(WALLET_ID)).thenReturn(Optional.of(wallet));
            when(transactionRepository.sumSignedAmount(any(), any())).thenReturn(BigDecimal.valueOf(1000));
            when(snapshotRepository.findLastTransactionId(any())).thenReturn(Optional.of(UUID.randomUUID()));
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            snapshotService.createSnapshotIfNeeded(WALLET_ID);

            // Then
            verify(snapshotRepository).save(any(BalanceSnapshot.class));
        }

        @Test
        @DisplayName("should not create snapshot when below threshold")
        void shouldNotCreateSnapshotWhenBelowThreshold() {
            // Given
            when(snapshotRepository.findTopByWalletIdOrderBySnapshotAtDesc(WALLET_ID))
                .thenReturn(Optional.empty());
            when(snapshotRepository.countTransactionsByWalletId(WALLET_ID))
                .thenReturn(50L); // < threshold (100)

            // When
            snapshotService.createSnapshotIfNeeded(WALLET_ID);

            // Then
            verify(snapshotRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("createSnapshot")
    class CreateSnapshotTests {

        @Test
        @DisplayName("should create and save snapshot")
        void shouldCreateAndSaveSnapshot() {
            // Given
            Wallet wallet = Wallet.create(createTestUser(), CURRENCY);
            
            when(walletRepository.findById(any())).thenReturn(Optional.of(wallet));
            when(transactionRepository.sumSignedAmount(any(), any())).thenReturn(BigDecimal.valueOf(1500));
            when(snapshotRepository.countTransactionsByWalletId(any())).thenReturn(75L);
            when(snapshotRepository.findLastTransactionId(any())).thenReturn(Optional.of(UUID.randomUUID()));
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            BalanceSnapshot result = snapshotService.createSnapshot(wallet.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBalance().amount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
            assertThat(result.getTransactionCount()).isEqualTo(75L);
            verify(snapshotRepository).save(any(BalanceSnapshot.class));
        }

        @Test
        @DisplayName("should throw exception when wallet not found")
        void shouldThrowExceptionWhenWalletNotFound() {
            // Given
            UUID walletId = UUID.randomUUID();
            when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> snapshotService.createSnapshot(walletId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wallet not found");
        }
    }
}
