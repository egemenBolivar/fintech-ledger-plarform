package com.ekup.fintech.ledger.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.ekup.fintech.shared.domain.Currency;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositRequest(
		@NotNull UUID walletId,
		@NotNull @Positive BigDecimal amount,
		@NotNull Currency currency,
		UUID idempotencyKey
) {
}
