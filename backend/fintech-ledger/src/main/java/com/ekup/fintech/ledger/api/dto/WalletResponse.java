package com.ekup.fintech.ledger.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.ekup.fintech.ledger.domain.WalletStatus;
import com.ekup.fintech.shared.domain.Currency;

public record WalletResponse(
		UUID id,
		UUID ownerId,
		Currency baseCurrency,
		WalletStatus status,
		Instant createdAt
) {
}
