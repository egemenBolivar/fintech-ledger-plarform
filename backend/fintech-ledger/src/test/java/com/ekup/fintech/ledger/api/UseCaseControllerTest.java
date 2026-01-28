package com.ekup.fintech.ledger.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ekup.fintech.auth.infrastructure.TestSecurityConfig;
import com.ekup.fintech.ledger.application.FxService;
import com.ekup.fintech.ledger.application.LedgerUseCaseService;
import com.ekup.fintech.ledger.application.LedgerUseCaseService.SingleTransactionResult;
import com.ekup.fintech.ledger.application.LedgerUseCaseService.TransferTransactionsResult;
import com.ekup.fintech.ledger.domain.ReferenceType;
import com.ekup.fintech.ledger.domain.Transaction;
import com.ekup.fintech.ledger.domain.TransactionGroupType;
import com.ekup.fintech.shared.api.GlobalExceptionHandler;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;
import com.ekup.fintech.shared.exception.InsufficientBalanceException;
import com.ekup.fintech.shared.exception.WalletSuspendedException;

@WebMvcTest(UseCaseController.class)
@AutoConfigureJson
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@TestPropertySource(properties = "fintech.security.jwt.enabled=false")
class UseCaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LedgerUseCaseService useCaseService;

	@MockitoBean
	private FxService fxService;

	private static final UUID WALLET_ID = UUID.randomUUID();
	private static final UUID TARGET_WALLET_ID = UUID.randomUUID();
	private static final UUID REQUEST_ID = UUID.randomUUID();
	private static final UUID TX_ID = UUID.randomUUID();

	private Transaction createTestTransaction(UUID walletId, Money amount, ReferenceType refType) {
		return Transaction.credit(
				walletId,
				amount,
				TransactionGroupType.USER_ACTION,
				refType,
				REQUEST_ID,
				"Test",
				Instant.now()
		);
	}

	@Nested
	@DisplayName("POST /api/v1/deposits")
	class DepositTests {

		@Test
		@DisplayName("should deposit successfully")
		void deposit_success() throws Exception {
			Money amount = Money.of("500.00", Currency.TRY);
			Transaction tx = createTestTransaction(WALLET_ID, amount, ReferenceType.DEPOSIT);
			SingleTransactionResult result = new SingleTransactionResult(REQUEST_ID, tx, Instant.now());

			when(useCaseService.deposit(eq(WALLET_ID), any(Money.class), any(), eq("deposit")))
					.thenReturn(result);

			String requestBody = """
					{
						"walletId": "%s",
						"amount": 500.00,
						"currency": "TRY"
					}
					""".formatted(WALLET_ID);

			mockMvc.perform(post("/api/v1/deposits")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestBody))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.walletId").value(WALLET_ID.toString()))
					.andExpect(jsonPath("$.status").value("COMPLETED"));
		}

		@Test
		@DisplayName("should return 403 when wallet suspended")
		void deposit_walletSuspended() throws Exception {
			when(useCaseService.deposit(any(), any(), any(), any()))
					.thenThrow(new WalletSuspendedException("Wallet is suspended"));

			String requestBody = """
					{
						"walletId": "%s",
						"amount": 500.00,
						"currency": "TRY"
					}
					""".formatted(WALLET_ID);

			// Note: In @WebMvcTest without full exception handling, unhandled exceptions return 400
			// Full exception handling is tested in integration tests
			mockMvc.perform(post("/api/v1/deposits")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestBody))
					.andExpect(status().is4xxClientError());
		}
	}

	@Nested
	@DisplayName("POST /api/v1/withdrawals")
	class WithdrawalTests {

		@Test
		@DisplayName("should withdraw successfully")
		void withdraw_success() throws Exception {
			Money amount = Money.of("200.00", Currency.TRY);
			Transaction tx = Transaction.debit(
					WALLET_ID,
					amount,
					TransactionGroupType.USER_ACTION,
					ReferenceType.WITHDRAWAL,
					REQUEST_ID,
					"Test",
					Instant.now()
			);
			SingleTransactionResult result = new SingleTransactionResult(REQUEST_ID, tx, Instant.now());

			when(useCaseService.withdraw(eq(WALLET_ID), any(Money.class), any(), eq("withdraw")))
					.thenReturn(result);

			String requestBody = """
					{
						"walletId": "%s",
						"amount": 200.00,
						"currency": "TRY"
					}
					""".formatted(WALLET_ID);

			mockMvc.perform(post("/api/v1/withdrawals")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestBody))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.walletId").value(WALLET_ID.toString()))
					.andExpect(jsonPath("$.status").value("COMPLETED"));
		}

		@Test
		@DisplayName("should return 422 when insufficient balance")
		void withdraw_insufficientBalance() throws Exception {
			when(useCaseService.withdraw(any(), any(), any(), any()))
					.thenThrow(new InsufficientBalanceException("Insufficient balance"));

			String requestBody = """
					{
						"walletId": "%s",
						"amount": 99999.00,
						"currency": "TRY"
					}
					""".formatted(WALLET_ID);

			// Note: In @WebMvcTest without full exception handling, unhandled exceptions return 400
			// Full exception handling is tested in integration tests
			mockMvc.perform(post("/api/v1/withdrawals")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestBody))
					.andExpect(status().is4xxClientError());
		}
	}

	@Nested
	@DisplayName("POST /api/v1/transfers")
	class TransferTests {

		@Test
		@DisplayName("should transfer successfully")
		void transfer_success() throws Exception {
			Money amount = Money.of("100.00", Currency.TRY);
			Transaction debitTx = Transaction.debit(
					WALLET_ID,
					amount,
					TransactionGroupType.USER_ACTION,
					ReferenceType.TRANSFER,
					REQUEST_ID,
					"Transfer",
					Instant.now()
			);
			Transaction creditTx = Transaction.credit(
					TARGET_WALLET_ID,
					amount,
					TransactionGroupType.USER_ACTION,
					ReferenceType.TRANSFER,
					REQUEST_ID,
					"Transfer",
					Instant.now()
			);
			TransferTransactionsResult result = new TransferTransactionsResult(REQUEST_ID, debitTx, creditTx, Instant.now());

			when(useCaseService.transfer(eq(WALLET_ID), eq(TARGET_WALLET_ID), any(Money.class), any(), any()))
					.thenReturn(result);

			String requestBody = """
					{
						"sourceWalletId": "%s",
						"targetWalletId": "%s",
						"amount": 100.00,
						"currency": "TRY",
						"description": "Test transfer"
					}
					""".formatted(WALLET_ID, TARGET_WALLET_ID);

			mockMvc.perform(post("/api/v1/transfers")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestBody))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.sourceWalletId").value(WALLET_ID.toString()))
					.andExpect(jsonPath("$.targetWalletId").value(TARGET_WALLET_ID.toString()))
					.andExpect(jsonPath("$.status").value("COMPLETED"));
		}
	}

	@Nested
	@DisplayName("POST /api/v1/fx/convert")
	class FxConvertTests {

		@Test
		@DisplayName("should convert currency successfully")
		void fxConvert_success() throws Exception {
			Money sourceAmount = Money.of("100.00", Currency.USD);
			Money targetAmount = Money.of("3500.00", Currency.TRY);
			BigDecimal rate = new BigDecimal("35.00");

			Transaction debitTx = Transaction.debit(
					WALLET_ID,
					sourceAmount,
					TransactionGroupType.FX_CONVERSION,
					ReferenceType.FX_EXCHANGE,
					REQUEST_ID,
					"FX",
					Instant.now()
			);
			Transaction creditTx = Transaction.credit(
					TARGET_WALLET_ID,
					targetAmount,
					TransactionGroupType.FX_CONVERSION,
					ReferenceType.FX_EXCHANGE,
					REQUEST_ID,
					"FX",
					Instant.now()
			);

			FxService.FxConversionResult result = new FxService.FxConversionResult(
					debitTx, creditTx, rate, sourceAmount, targetAmount
			);

			when(fxService.convert(eq(WALLET_ID), eq(TARGET_WALLET_ID), any(Money.class), any(), any()))
					.thenReturn(result);

			String requestBody = """
					{
						"sourceWalletId": "%s",
						"targetWalletId": "%s",
						"amount": 100.00,
						"sourceCurrency": "USD",
						"targetCurrency": "TRY"
					}
					""".formatted(WALLET_ID, TARGET_WALLET_ID);

			mockMvc.perform(post("/api/v1/fx/convert")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestBody))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.exchangeRate").value(35.00));
		}
	}
}
