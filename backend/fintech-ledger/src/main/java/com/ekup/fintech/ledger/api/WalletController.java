package com.ekup.fintech.ledger.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ekup.fintech.auth.domain.User;
import com.ekup.fintech.ledger.api.dto.BalanceResponse;
import com.ekup.fintech.ledger.api.dto.CreateWalletRequest;
import com.ekup.fintech.ledger.api.dto.WalletResponse;
import com.ekup.fintech.ledger.application.BalanceCalculator;
import com.ekup.fintech.ledger.application.WalletService;
import com.ekup.fintech.ledger.domain.Wallet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Wallet management operations")
public class WalletController {
	private final WalletService walletService;
	private final BalanceCalculator balanceCalculator;

	public WalletController(WalletService walletService, BalanceCalculator balanceCalculator) {
		this.walletService = walletService;
		this.balanceCalculator = balanceCalculator;
	}

	@Operation(summary = "Create wallet", description = "Create a new wallet for the authenticated user with specified base currency")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WalletResponse create(@Valid @RequestBody CreateWalletRequest request, @AuthenticationPrincipal User currentUser) {
		Wallet wallet = walletService.createWallet(currentUser, request.baseCurrency());
		return toResponse(wallet);
	}

	@Operation(summary = "List my wallets", description = "Retrieve all wallets owned by the authenticated user")
	@GetMapping
	public List<WalletResponse> listMyWallets(@AuthenticationPrincipal User currentUser) {
		return walletService.getWalletsForUser(currentUser).stream().map(WalletController::toResponse).toList();
	}

	@Operation(summary = "Get wallet", description = "Retrieve wallet details by ID (must be owned by authenticated user)")
	@GetMapping("/{id}")
	public WalletResponse get(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
		return toResponse(walletService.getWalletForUser(id, currentUser));
	}

	@Operation(summary = "Get balance", description = "Calculate current balance from transaction history")
	@GetMapping("/{id}/balance")
	public BalanceResponse balance(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
		Wallet wallet = walletService.getWalletForUser(id, currentUser);
		return new BalanceResponse(wallet.getId(), balanceCalculator.calculateBalance(id, wallet.getBaseCurrency()), Instant.now());
	}

	@Operation(summary = "Suspend wallet", description = "Temporarily suspend a wallet from transacting")
	@PatchMapping("/{id}/suspend")
	public WalletResponse suspend(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
		return toResponse(walletService.suspend(id, currentUser));
	}

	@Operation(summary = "Activate wallet", description = "Re-activate a suspended wallet")
	@PatchMapping("/{id}/activate")
	public WalletResponse activate(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
		return toResponse(walletService.activate(id, currentUser));
	}

	private static WalletResponse toResponse(Wallet wallet) {
		return new WalletResponse(wallet.getId(), wallet.getOwnerId(), wallet.getBaseCurrency(), wallet.getStatus(), wallet.getCreatedAt());
	}
}
