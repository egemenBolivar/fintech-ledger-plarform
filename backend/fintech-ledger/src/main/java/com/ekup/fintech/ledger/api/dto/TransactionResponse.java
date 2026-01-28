package com.ekup.fintech.ledger.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.ekup.fintech.ledger.domain.ReferenceType;
import com.ekup.fintech.ledger.domain.TransactionDirection;
import com.ekup.fintech.ledger.domain.TransactionGroupType;
import com.ekup.fintech.shared.domain.Money;

public record TransactionResponse(
		UUID id,
		UUID walletId,
		Money amount,
		TransactionDirection direction,
		TransactionGroupType groupType,
		ReferenceType referenceType,
		UUID referenceId,
		String description,
		Instant occurredAt
) {
}
