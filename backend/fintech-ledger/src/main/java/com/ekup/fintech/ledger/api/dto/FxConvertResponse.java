package com.ekup.fintech.ledger.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ekup.fintech.shared.domain.Money;

public record FxConvertResponse(
		UUID conversionId,
		UUID debitTransactionId,
		UUID creditTransactionId,
		Money sourceAmount,
		Money targetAmount,
		BigDecimal exchangeRate,
		Instant processedAt
) {
}
