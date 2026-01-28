package com.ekup.fintech.ledger.domain;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ekup.fintech.auth.domain.Role;
import com.ekup.fintech.auth.domain.User;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.exception.WalletClosedException;

class WalletTest {

	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = User.create("test@example.com", "password", "Test User", Set.of(Role.USER));
	}

	@Test
	void createWalletHasActiveStatus() {
		Wallet wallet = Wallet.create(testUser, Currency.USD);

		assertThat(wallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
		assertThat(wallet.getBaseCurrency()).isEqualTo(Currency.USD);
		assertThat(wallet.getId()).isNotNull();
		assertThat(wallet.getOwner()).isEqualTo(testUser);
	}

	@Test
	void suspendChangesStatusToSuspended() {
		Wallet wallet = Wallet.create(testUser, Currency.EUR);

		wallet.suspend();

		assertThat(wallet.getStatus()).isEqualTo(WalletStatus.SUSPENDED);
	}

	@Test
	void activateChangesStatusBackToActive() {
		Wallet wallet = Wallet.create(testUser, Currency.EUR);
		wallet.suspend();

		wallet.activate();

		assertThat(wallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
	}

	@Test
	void closeChangesStatusToClosed() {
		Wallet wallet = Wallet.create(testUser, Currency.GBP);

		wallet.close();

		assertThat(wallet.getStatus()).isEqualTo(WalletStatus.CLOSED);
	}

	@Test
	void suspendOnClosedWalletThrows() {
		Wallet wallet = Wallet.create(testUser, Currency.TRY);
		wallet.close();

		assertThatThrownBy(wallet::suspend)
				.isInstanceOf(WalletClosedException.class);
	}

	@Test
	void activateOnClosedWalletThrows() {
		Wallet wallet = Wallet.create(testUser, Currency.TRY);
		wallet.close();

		assertThatThrownBy(wallet::activate)
				.isInstanceOf(WalletClosedException.class);
	}
}
