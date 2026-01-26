package com.ekup.fintech.ledger.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ekup.fintech.ledger.domain.Wallet;
import com.ekup.fintech.ledger.infrastructure.persistence.WalletJpaRepository;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.exception.DuplicateWalletException;

@Service
public class WalletService {
	private final WalletJpaRepository walletRepository;

	public WalletService(WalletJpaRepository walletRepository) {
		this.walletRepository = walletRepository;
	}

	@Transactional
	public Wallet createWallet(UUID ownerId, Currency baseCurrency) {
		if (walletRepository.existsByOwnerIdAndBaseCurrency(ownerId, baseCurrency)) {
			throw new DuplicateWalletException("Owner already has a wallet for currency " + baseCurrency);
		}
		return walletRepository.save(Wallet.create(ownerId, baseCurrency));
	}

	@Transactional(readOnly = true)
	public Wallet getWallet(UUID id) {
		return walletRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
	}

	@Transactional
	public Wallet suspend(UUID id) {
		Wallet wallet = getWallet(id);
		wallet.suspend();
		return wallet;
	}

	@Transactional
	public Wallet activate(UUID id) {
		Wallet wallet = getWallet(id);
		wallet.activate();
		return wallet;
	}
}
