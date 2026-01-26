package com.ekup.fintech.ledger.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ekup.fintech.ledger.domain.Wallet;
import com.ekup.fintech.shared.domain.Currency;

public interface WalletJpaRepository extends JpaRepository<Wallet, UUID> {
	boolean existsByOwnerIdAndBaseCurrency(UUID ownerId, Currency baseCurrency);

	Optional<Wallet> findByOwnerIdAndBaseCurrency(UUID ownerId, Currency baseCurrency);
}
