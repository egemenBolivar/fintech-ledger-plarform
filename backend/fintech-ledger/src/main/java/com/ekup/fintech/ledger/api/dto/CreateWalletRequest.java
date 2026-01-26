package com.ekup.fintech.ledger.api.dto;

import java.util.UUID;

import com.ekup.fintech.shared.domain.Currency;

import jakarta.validation.constraints.NotNull;

public record CreateWalletRequest(
		@NotNull UUID ownerId,
		@NotNull Currency baseCurrency
) {
}
