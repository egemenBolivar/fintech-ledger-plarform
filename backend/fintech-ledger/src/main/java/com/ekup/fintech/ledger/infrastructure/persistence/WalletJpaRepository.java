package com.ekup.fintech.ledger.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ekup.fintech.auth.domain.User;
import com.ekup.fintech.ledger.domain.Wallet;
import com.ekup.fintech.shared.domain.Currency;

public interface WalletJpaRepository extends JpaRepository<Wallet, UUID> {
	boolean existsByOwnerAndBaseCurrency(User owner, Currency baseCurrency);

	Optional<Wallet> findByOwnerAndBaseCurrency(User owner, Currency baseCurrency);

	List<Wallet> findByOwner(User owner);
}
