package com.ekup.fintech.ledger.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ekup.fintech.auth.domain.Role;
import com.ekup.fintech.auth.domain.User;
import com.ekup.fintech.auth.infrastructure.TestSecurityConfig;
import com.ekup.fintech.ledger.application.BalanceCalculator;
import com.ekup.fintech.ledger.application.WalletService;
import com.ekup.fintech.ledger.domain.Wallet;
import com.ekup.fintech.shared.api.GlobalExceptionHandler;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;
import com.ekup.fintech.shared.exception.ResourceNotFoundException;

@WebMvcTest(WalletController.class)
@AutoConfigureJson
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@TestPropertySource(properties = "fintech.security.jwt.enabled=false")
class WalletControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WalletService walletService;

	@MockitoBean
	private BalanceCalculator balanceCalculator;

	private static final UUID WALLET_ID = UUID.randomUUID();
	private static final UUID OWNER_ID = UUID.randomUUID();

	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = User.create("test@example.com", "password", "Test User", Set.of(Role.USER));
		try {
			var idField = User.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(testUser, OWNER_ID);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Wallet createTestWallet() {
		Wallet wallet = Wallet.create(testUser, Currency.TRY);
		try {
			var idField = Wallet.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(wallet, WALLET_ID);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return wallet;
	}

	@Nested
	@DisplayName("POST /api/v1/wallets")
	class CreateWalletTests {

		@Test
		@DisplayName("should create wallet successfully")
		@WithMockUser
		void createWallet_success() throws Exception {
			Wallet wallet = createTestWallet();
			when(walletService.createWallet(any(User.class), eq(Currency.TRY))).thenReturn(wallet);

			String requestBody = """
					{
						"baseCurrency": "TRY"
					}
					""";

			mockMvc.perform(post("/api/v1/wallets")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestBody))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.id").value(WALLET_ID.toString()))
					.andExpect(jsonPath("$.ownerId").value(OWNER_ID.toString()))
					.andExpect(jsonPath("$.baseCurrency").value("TRY"))
					.andExpect(jsonPath("$.status").value("ACTIVE"));
		}

		@Test
		@DisplayName("should fail with invalid request")
		@WithMockUser
		void createWallet_invalidRequest() throws Exception {
			String requestBody = """
					{
						"baseCurrency": null
					}
					""";

			mockMvc.perform(post("/api/v1/wallets")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestBody))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("GET /api/v1/wallets/{id}")
	class GetWalletTests {

		@Test
		@DisplayName("should return wallet by id")
		@WithMockUser
		void getWallet_success() throws Exception {
			Wallet wallet = createTestWallet();
			when(walletService.getWalletForUser(eq(WALLET_ID), any(User.class))).thenReturn(wallet);

			mockMvc.perform(get("/api/v1/wallets/{id}", WALLET_ID))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(WALLET_ID.toString()))
					.andExpect(jsonPath("$.ownerId").value(OWNER_ID.toString()));
		}

		@Test
		@DisplayName("should return 404 when wallet not found")
		@WithMockUser
		void getWallet_notFound() throws Exception {
			when(walletService.getWalletForUser(eq(WALLET_ID), any(User.class)))
					.thenThrow(new ResourceNotFoundException("Wallet not found: " + WALLET_ID));

			mockMvc.perform(get("/api/v1/wallets/{id}", WALLET_ID))
					.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("GET /api/v1/wallets/{id}/balance")
	class GetBalanceTests {

		@Test
		@DisplayName("should return balance")
		@WithMockUser
		void getBalance_success() throws Exception {
			Wallet wallet = createTestWallet();
			when(walletService.getWalletForUser(eq(WALLET_ID), any(User.class))).thenReturn(wallet);
			when(balanceCalculator.calculateBalance(WALLET_ID, Currency.TRY))
					.thenReturn(Money.of("1000.50", Currency.TRY));

			mockMvc.perform(get("/api/v1/wallets/{id}/balance", WALLET_ID))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.walletId").value(WALLET_ID.toString()))
					.andExpect(jsonPath("$.balance.amount").value(1000.50))
					.andExpect(jsonPath("$.balance.currency").value("TRY"));
		}
	}

	@Nested
	@DisplayName("GET /api/v1/wallets")
	class ListWalletsTests {

		@Test
		@DisplayName("should list user's wallets")
		@WithMockUser
		void listWallets_success() throws Exception {
			Wallet wallet = createTestWallet();
			when(walletService.getWalletsForUser(any(User.class))).thenReturn(List.of(wallet));

			mockMvc.perform(get("/api/v1/wallets"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$").isArray())
					.andExpect(jsonPath("$[0].id").value(WALLET_ID.toString()));
		}
	}

	@Nested
	@DisplayName("PATCH /api/v1/wallets/{id}/suspend")
	class SuspendWalletTests {

		@Test
		@DisplayName("should suspend wallet")
		@WithMockUser
		void suspendWallet_success() throws Exception {
			Wallet wallet = createTestWallet();
			wallet.suspend();
			when(walletService.suspend(eq(WALLET_ID), any(User.class))).thenReturn(wallet);

			mockMvc.perform(patch("/api/v1/wallets/{id}/suspend", WALLET_ID))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("SUSPENDED"));
		}
	}

	@Nested
	@DisplayName("PATCH /api/v1/wallets/{id}/activate")
	class ActivateWalletTests {

		@Test
		@DisplayName("should activate wallet")
		@WithMockUser
		void activateWallet_success() throws Exception {
			Wallet wallet = createTestWallet();
			when(walletService.activate(eq(WALLET_ID), any(User.class))).thenReturn(wallet);

			mockMvc.perform(patch("/api/v1/wallets/{id}/activate", WALLET_ID))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("ACTIVE"));
		}
	}
}
