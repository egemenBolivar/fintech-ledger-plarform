package com.ekup.fintech.ledger.api.dto;

import com.ekup.fintech.shared.domain.Currency;

import jakarta.validation.constraints.NotNull;

public record CreateWalletRequest(
		@NotNull Currency baseCurrency
) {
}
