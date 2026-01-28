package com.ekup.fintech.ledger.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.ekup.fintech.shared.domain.Money;

public record WithdrawalResponse(
		UUID withdrawalId,
		UUID transactionId,
		UUID walletId,
		Money amount,
		String status,
		Instant processedAt
) {
}
