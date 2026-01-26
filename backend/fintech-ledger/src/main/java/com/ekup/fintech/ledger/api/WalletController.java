package com.ekup.fintech.ledger.api;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ekup.fintech.ledger.api.dto.BalanceResponse;
import com.ekup.fintech.ledger.api.dto.CreateWalletRequest;
import com.ekup.fintech.ledger.api.dto.WalletResponse;
import com.ekup.fintech.ledger.application.BalanceCalculator;
import com.ekup.fintech.ledger.application.WalletService;
import com.ekup.fintech.ledger.domain.Wallet;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
	private final WalletService walletService;
	private final BalanceCalculator balanceCalculator;

	public WalletController(WalletService walletService, BalanceCalculator balanceCalculator) {
		this.walletService = walletService;
		this.balanceCalculator = balanceCalculator;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WalletResponse create(@Valid @RequestBody CreateWalletRequest request) {
		Wallet wallet = walletService.createWallet(request.ownerId(), request.baseCurrency());
		return toResponse(wallet);
	}

	@GetMapping("/{id}")
	public WalletResponse get(@PathVariable UUID id) {
		return toResponse(walletService.getWallet(id));
	}

	@GetMapping("/{id}/balance")
	public BalanceResponse balance(@PathVariable UUID id) {
		Wallet wallet = walletService.getWallet(id);
		return new BalanceResponse(wallet.getId(), balanceCalculator.calculateBalance(id, wallet.getBaseCurrency()), Instant.now());
	}

	@PatchMapping("/{id}/suspend")
	public WalletResponse suspend(@PathVariable UUID id) {
		return toResponse(walletService.suspend(id));
	}

	@PatchMapping("/{id}/activate")
	public WalletResponse activate(@PathVariable UUID id) {
		return toResponse(walletService.activate(id));
	}

	private static WalletResponse toResponse(Wallet wallet) {
		return new WalletResponse(wallet.getId(), wallet.getOwnerId(), wallet.getBaseCurrency(), wallet.getStatus(), wallet.getCreatedAt());
	}
}
