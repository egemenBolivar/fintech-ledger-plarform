package com.ekup.fintech.ledger.api;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ekup.fintech.ledger.api.dto.DepositRequest;
import com.ekup.fintech.ledger.api.dto.DepositResponse;
import com.ekup.fintech.ledger.api.dto.FxConvertRequest;
import com.ekup.fintech.ledger.api.dto.FxConvertResponse;
import com.ekup.fintech.ledger.api.dto.FxRateResponse;
import com.ekup.fintech.ledger.api.dto.TransferRequest;
import com.ekup.fintech.ledger.api.dto.TransferResponse;
import com.ekup.fintech.ledger.api.dto.WithdrawalRequest;
import com.ekup.fintech.ledger.api.dto.WithdrawalResponse;
import com.ekup.fintech.ledger.application.FxRateProvider;
import com.ekup.fintech.ledger.application.FxService;
import com.ekup.fintech.ledger.application.LedgerUseCaseService;
import com.ekup.fintech.ledger.application.LedgerUseCaseService.SingleTransactionResult;
import com.ekup.fintech.ledger.application.LedgerUseCaseService.TransferTransactionsResult;
import com.ekup.fintech.ledger.domain.Transaction;
import com.ekup.fintech.shared.domain.Currency;
import com.ekup.fintech.shared.domain.Money;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Operations", description = "Financial operations: deposits, withdrawals, transfers, FX")
public class UseCaseController {
	private final LedgerUseCaseService useCaseService;
	private final FxService fxService;
	private final FxRateProvider fxRateProvider;

	public UseCaseController(LedgerUseCaseService useCaseService, FxService fxService, FxRateProvider fxRateProvider) {
		this.useCaseService = useCaseService;
		this.fxService = fxService;
		this.fxRateProvider = fxRateProvider;
	}

	@Operation(summary = "Get FX rate", description = "Get exchange rate and preview converted amount between two currencies")
	@GetMapping("/fx/rate")
	public FxRateResponse getFxRate(
			@RequestParam Currency from,
			@RequestParam Currency to,
			@RequestParam BigDecimal amount) {
		BigDecimal rate = fxRateProvider.getRate(from, to);
		BigDecimal convertedAmount = amount.multiply(rate);
		return new FxRateResponse(from, to, rate, amount, convertedAmount);
	}

	@Operation(summary = "Deposit funds", description = "Add money to a wallet. Creates a CREDIT transaction.")
	@PostMapping("/deposits")
	@ResponseStatus(HttpStatus.OK)
	public DepositResponse deposit(@Valid @RequestBody DepositRequest request) {
		SingleTransactionResult result = useCaseService.deposit(
				request.walletId(),
				Money.of(request.amount(), request.currency()),
				request.idempotencyKey(),
				"deposit"
		);
		Transaction tx = result.transaction();
		return new DepositResponse(result.requestId(), tx.getId(), tx.getWalletId(), tx.getMoney(), "COMPLETED", result.processedAt());
	}

	@Operation(summary = "Withdraw funds", description = "Remove money from a wallet. Creates a DEBIT transaction. Fails if insufficient balance.")
	@PostMapping("/withdrawals")
	@ResponseStatus(HttpStatus.OK)
	public WithdrawalResponse withdraw(@Valid @RequestBody WithdrawalRequest request) {
		SingleTransactionResult result = useCaseService.withdraw(
				request.walletId(),
				Money.of(request.amount(), request.currency()),
				request.idempotencyKey(),
				"withdraw"
		);
		Transaction tx = result.transaction();
		return new WithdrawalResponse(result.requestId(), tx.getId(), tx.getWalletId(), tx.getMoney(), "COMPLETED", result.processedAt());
	}

	@Operation(summary = "Transfer funds", description = "Move money between two wallets. Creates DEBIT on source and CREDIT on target atomically.")
	@PostMapping("/transfers")
	@ResponseStatus(HttpStatus.OK)
	public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
		TransferTransactionsResult result = useCaseService.transfer(
				request.sourceWalletId(),
				request.targetWalletId(),
				Money.of(request.amount(), request.currency()),
				request.idempotencyKey(),
				request.description() != null ? request.description() : "transfer"
		);
		return new TransferResponse(
				result.requestId(),
				result.sourceTransaction().getId(),
				result.targetTransaction().getId(),
				request.sourceWalletId(),
				request.targetWalletId(),
				Money.of(request.amount(), request.currency()),
				"COMPLETED",
				result.processedAt()
		);
	}

	@Operation(summary = "FX conversion", description = "Convert currency between two wallets with different base currencies using mock exchange rates.")
	@PostMapping("/fx/convert")
	@ResponseStatus(HttpStatus.OK)
	public FxConvertResponse fxConvert(@Valid @RequestBody FxConvertRequest request) {
		FxService.FxConversionResult result = fxService.convert(
				request.sourceWalletId(),
				request.targetWalletId(),
				Money.of(request.amount(), request.sourceCurrency()),
				request.idempotencyKey(),
				"FX conversion"
		);
		return new FxConvertResponse(
				request.idempotencyKey(),
				result.debitTransaction().getId(),
				result.creditTransaction().getId(),
				result.sourceAmount(),
				result.targetAmount(),
				result.exchangeRate(),
				result.debitTransaction().getOccurredAt()
		);
	}
}
