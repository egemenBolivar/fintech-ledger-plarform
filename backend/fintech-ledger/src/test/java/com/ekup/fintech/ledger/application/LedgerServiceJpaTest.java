package com.ekup.fintech.ledger.application;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.ekup.fintech.shared.exception.InsufficientBalanceException;
import com.ekup.fintech.shared.exception.SameWalletTransferException;

@SpringBootTest
@Transactional
class LedgerServiceJpaTest {
	@Autowired
	WalletJpaRepository walletRepository;

	@Autowired
	LedgerService ledgerService;

	@Autowired
	BalanceCalculator balanceCalculator;

	@Autowired
	UserRepository userRepository;

	private User testUser;
	private User otherUser;

	@BeforeEach
	void setUp() {
		testUser = userRepository.save(User.create("ledger-test@example.com", "password", "Test User", Set.of(Role.USER)));
		otherUser = userRepository.save(User.create("ledger-other@example.com", "password", "Other User", Set.of(Role.USER)));
	}

	@Test
	void depositIncreasesBalance() {
		Wallet wallet = walletRepository.save(Wallet.create(testUser, Currency.USD));

		ledgerService.deposit(wallet.getId(), Money.of(new BigDecimal("100"), Currency.USD), UUID.randomUUID(), "test");

		Money balance = balanceCalculator.calculateBalance(wallet.getId(), Currency.USD);
		assertThat(balance.amount()).isEqualByComparingTo(new BigDecimal("100"));
	}

	@Test
	void withdrawDecreasesBalance() {
		Wallet wallet = walletRepository.save(Wallet.create(testUser, Currency.USD));
		ledgerService.deposit(wallet.getId(), Money.of(new BigDecimal("100"), Currency.USD), UUID.randomUUID(), "deposit");

		ledgerService.withdraw(wallet.getId(), Money.of(new BigDecimal("30"), Currency.USD), UUID.randomUUID(), "withdraw");

		Money balance = balanceCalculator.calculateBalance(wallet.getId(), Currency.USD);
		assertThat(balance.amount()).isEqualByComparingTo(new BigDecimal("70"));
	}

	@Test
	void withdrawFailsOnInsufficientBalance() {
		Wallet wallet = walletRepository.save(Wallet.create(testUser, Currency.USD));
		ledgerService.deposit(wallet.getId(), Money.of(new BigDecimal("50"), Currency.USD), UUID.randomUUID(), "deposit");

		assertThatThrownBy(() ->
				ledgerService.withdraw(wallet.getId(), Money.of(new BigDecimal("100"), Currency.USD), UUID.randomUUID(), "withdraw")
		).isInstanceOf(InsufficientBalanceException.class);
	}

	@Test
	void transferMovesMoneyBetweenWallets() {
		Wallet source = walletRepository.save(Wallet.create(testUser, Currency.USD));
		Wallet target = walletRepository.save(Wallet.create(otherUser, Currency.USD));
		ledgerService.deposit(source.getId(), Money.of(new BigDecimal("200"), Currency.USD), UUID.randomUUID(), "deposit");

		ledgerService.transfer(source.getId(), target.getId(), Money.of(new BigDecimal("75"), Currency.USD), UUID.randomUUID(), "transfer");

		assertThat(balanceCalculator.calculateBalance(source.getId(), Currency.USD).amount())
				.isEqualByComparingTo(new BigDecimal("125"));
		assertThat(balanceCalculator.calculateBalance(target.getId(), Currency.USD).amount())
				.isEqualByComparingTo(new BigDecimal("75"));
	}

	@Test
	void transferToSameWalletFails() {
		Wallet wallet = walletRepository.save(Wallet.create(testUser, Currency.USD));
		ledgerService.deposit(wallet.getId(), Money.of(new BigDecimal("100"), Currency.USD), UUID.randomUUID(), "deposit");

		assertThatThrownBy(() ->
				ledgerService.transfer(wallet.getId(), wallet.getId(), Money.of(new BigDecimal("50"), Currency.USD), UUID.randomUUID(), "self transfer")
		).isInstanceOf(SameWalletTransferException.class);
	}
}
