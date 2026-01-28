package com.ekup.fintech.ledger.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.ekup.fintech.shared.domain.Currency;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
		@NotNull UUID sourceWalletId,
		@NotNull UUID targetWalletId,
		@NotNull @Positive BigDecimal amount,
		@NotNull Currency currency,
		String description,
		UUID idempotencyKey
) {
}
