package com.ekup.fintech.ledger.api.dto;

import java.math.BigDecimal;

import com.ekup.fintech.shared.domain.Currency;

public record FxRateResponse(
		Currency fromCurrency,
		Currency toCurrency,
		BigDecimal rate,
		BigDecimal sourceAmount,
		BigDecimal targetAmount
) {}
