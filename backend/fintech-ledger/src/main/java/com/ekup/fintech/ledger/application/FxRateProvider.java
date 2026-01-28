package com.ekup.fintech.ledger.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;

/**
 * Mock FX rate provider for demo purposes.
 * In production, this would call an external FX API (e.g., Open Exchange Rates, Fixer, etc.)
 */
@Service
public class FxRateProvider {
	// Mock rates relative to USD (base currency)
	private static final Map<Currency, BigDecimal> RATES_TO_USD = Map.of(
			Currency.USD, BigDecimal.ONE,
			Currency.EUR, new BigDecimal("0.92"),    // 1 USD = 0.92 EUR
			Currency.GBP, new BigDecimal("0.79"),    // 1 USD = 0.79 GBP
			Currency.TRY, new BigDecimal("32.50")    // 1 USD = 32.50 TRY
	);

	/**
	 * Get exchange rate from source currency to target currency.
	 * Example: getRate(USD, EUR) returns ~0.92
	 */
	public BigDecimal getRate(Currency source, Currency target) {
		if (source == target) {
			return BigDecimal.ONE;
		}
		// Convert source → USD → target
		BigDecimal sourceToUsd = BigDecimal.ONE.divide(RATES_TO_USD.get(source), 10, RoundingMode.HALF_UP);
		BigDecimal usdToTarget = RATES_TO_USD.get(target);
		return sourceToUsd.multiply(usdToTarget).setScale(6, RoundingMode.HALF_UP);
	}

	/**
	 * Convert an amount from source currency to target currency.
	 */
	public Money convert(Money source, Currency targetCurrency) {
		if (source.currency() == targetCurrency) {
			return source;
		}
		BigDecimal rate = getRate(source.currency(), targetCurrency);
		BigDecimal converted = source.amount().multiply(rate);
		return Money.of(converted, targetCurrency);
	}
}
