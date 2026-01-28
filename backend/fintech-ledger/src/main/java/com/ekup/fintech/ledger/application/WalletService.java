package com.ekup.fintech.ledger.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ekup.fintech.auth.domain.User;
import com.ekup.fintech.ledger.domain.Wallet;
import com.ekup.fintech.ledger.infrastructure.persistence.WalletJpaRepository;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.exception.AccessDeniedException;
import com.ekup.fintech.shared.exception.DuplicateWalletException;
import com.ekup.fintech.shared.exception.ResourceNotFoundException;

@Service
public class WalletService {
	private final WalletJpaRepository walletRepository;

	public WalletService(WalletJpaRepository walletRepository) {
		this.walletRepository = walletRepository;
	}

	@Transactional
	public Wallet createWallet(User owner, Currency baseCurrency) {
		if (walletRepository.existsByOwnerAndBaseCurrency(owner, baseCurrency)) {
			throw new DuplicateWalletException("You already have a wallet for currency " + baseCurrency);
		}
		return walletRepository.save(Wallet.create(owner, baseCurrency));
	}

	@Transactional(readOnly = true)
	public Wallet getWallet(UUID id) {
		return walletRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + id));
	}

	@Transactional(readOnly = true)
	public Wallet getWalletForUser(UUID walletId, User user) {
		Wallet wallet = getWallet(walletId);
		ensureOwnership(wallet, user);
		return wallet;
	}

	@Transactional(readOnly = true)
	public List<Wallet> getWalletsForUser(User user) {
		return walletRepository.findByOwner(user);
	}

	@Transactional(readOnly = true)
	public List<Wallet> getAllWallets() {
		return walletRepository.findAll();
	}

	@Transactional
	public Wallet suspend(UUID id, User user) {
		Wallet wallet = getWallet(id);
		ensureOwnership(wallet, user);
		wallet.suspend();
		return wallet;
	}

	@Transactional
	public Wallet activate(UUID id, User user) {
		Wallet wallet = getWallet(id);
		ensureOwnership(wallet, user);
		wallet.activate();
		return wallet;
	}

	private void ensureOwnership(Wallet wallet, User user) {
		if (!wallet.getOwnerId().equals(user.getId())) {
			throw new AccessDeniedException("You don't have access to this wallet");
		}
	}
}
