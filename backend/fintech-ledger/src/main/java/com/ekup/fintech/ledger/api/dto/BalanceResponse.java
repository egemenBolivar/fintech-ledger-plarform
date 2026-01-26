package com.ekup.fintech.ledger.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.ekup.fintech.shared.domain.Money;

public record BalanceResponse(
		UUID walletId,
		Money balance,
		Instant calculatedAt
) {
}
