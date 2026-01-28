package com.ekup.fintech.ledger.application;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ekup.fintech.auth.domain.Role;
import com.ekup.fintech.auth.domain.User;
import com.ekup.fintech.auth.infrastructure.UserRepository;
import com.ekup.fintech.ledger.domain.Wallet;
import com.ekup.fintech.ledger.infrastructure.persistence.WalletJpaRepository;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;

@SpringBootTest
@Transactional
class FxServiceJpaTest {
	@Autowired
	WalletJpaRepository walletRepository;

	@Autowired
	LedgerService ledgerService;

	@Autowired
	FxService fxService;

	@Autowired
	BalanceCalculator balanceCalculator;

	@Autowired
	UserRepository userRepository;

	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = userRepository.save(User.create("fx-test@example.com", "password", "Test User", Set.of(Role.USER)));
	}

	@Test
	void fxConversionDebitsSourceAndCreditsTarget() {
		Wallet usdWallet = walletRepository.save(Wallet.create(testUser, Currency.USD));
		Wallet eurWallet = walletRepository.save(Wallet.create(testUser, Currency.EUR));

		// Deposit 100 USD
		ledgerService.deposit(usdWallet.getId(), Money.of(new BigDecimal("100"), Currency.USD), UUID.randomUUID(), "deposit");

		// Convert 50 USD to EUR
		FxService.FxConversionResult result = fxService.convert(
				usdWallet.getId(),
				eurWallet.getId(),
				Money.of(new BigDecimal("50"), Currency.USD),
				UUID.randomUUID(),
				"test fx"
		);

		// Verify source debited
		Money usdBalance = balanceCalculator.calculateBalance(usdWallet.getId(), Currency.USD);
		assertThat(usdBalance.amount()).isEqualByComparingTo(new BigDecimal("50"));

		// Verify target credited (50 USD * 0.92 rate = 46 EUR approximately)
		Money eurBalance = balanceCalculator.calculateBalance(eurWallet.getId(), Currency.EUR);
		assertThat(eurBalance.amount()).isGreaterThan(BigDecimal.ZERO);
		assertThat(result.exchangeRate()).isNotNull();
		assertThat(result.targetAmount().currency()).isEqualTo(Currency.EUR);
	}
}
