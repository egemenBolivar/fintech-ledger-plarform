package com.ekup.fintech.ledger.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.ekup.fintech.shared.domain.Money;

public record TransferResponse(
		UUID transferId,
		UUID sourceTransactionId,
		UUID targetTransactionId,
		UUID sourceWalletId,
		UUID targetWalletId,
		Money amount,
		String status,
		Instant processedAt
) {
}
