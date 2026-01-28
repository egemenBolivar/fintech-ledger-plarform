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
import com.ekup.fintech.ledger.infrastructure.persistence.TransactionJpaRepository;
import com.ekup.fintech.ledger.infrastructure.persistence.WalletJpaRepository;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;

@SpringBootTest
@Transactional
class IdempotencyDepositJpaTest {
	@Autowired
	WalletJpaRepository walletRepository;

	@Autowired
	TransactionJpaRepository transactionRepository;

	@Autowired
	LedgerUseCaseService useCaseService;

	@Autowired
	UserRepository userRepository;

	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = userRepository.save(User.create("idempotency-test@example.com", "password", "Test User", Set.of(Role.USER)));
	}

	@Test
	void depositWithSameIdempotencyKeyDoesNotCreateDuplicateTransaction() {
		Wallet wallet = walletRepository.save(Wallet.create(testUser, Currency.USD));
		UUID key = UUID.randomUUID();

		var first = useCaseService.deposit(wallet.getId(), Money.of(new BigDecimal("10"), Currency.USD), key, "deposit");
		var second = useCaseService.deposit(wallet.getId(), Money.of(new BigDecimal("10"), Currency.USD), key, "deposit");

		assertThat(first.transaction().getId()).isEqualTo(second.transaction().getId());
		assertThat(transactionRepository.count()).isEqualTo(1);
	}
}
