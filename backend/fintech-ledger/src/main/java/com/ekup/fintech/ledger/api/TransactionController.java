package com.ekup.fintech.ledger.api;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ekup.fintech.ledger.api.dto.TransactionResponse;
import com.ekup.fintech.ledger.domain.ReferenceType;
import com.ekup.fintech.ledger.domain.Transaction;
import com.ekup.fintech.ledger.domain.TransactionDirection;
import com.ekup.fintech.ledger.domain.TransactionGroupType;
import com.ekup.fintech.ledger.infrastructure.persistence.TransactionJpaRepository;
import com.ekup.fintech.shared.exception.ResourceNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Transactions", description = "Transaction history and details")
public class TransactionController {
	private final TransactionJpaRepository transactionRepository;

	public TransactionController(TransactionJpaRepository transactionRepository) {
		this.transactionRepository = transactionRepository;
	}

	@Operation(summary = "List wallet transactions", description = "Retrieve paginated transaction history for a wallet with optional filters")
	@GetMapping("/wallets/{walletId}/transactions")
	public Page<TransactionResponse> listWalletTransactions(
			@PathVariable UUID walletId,
			@Parameter(description = "Filter by CREDIT or DEBIT") @RequestParam(required = false) TransactionDirection direction,
			@Parameter(description = "Filter by group type") @RequestParam(required = false) TransactionGroupType groupType,
			@Parameter(description = "Filter by reference type") @RequestParam(required = false) ReferenceType referenceType,
			@Parameter(description = "Start date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant from,
			@Parameter(description = "End date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant to,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return transactionRepository.searchByWalletId(walletId, direction, groupType, referenceType, from, to, pageable)
				.map(TransactionController::toResponse);
	}

	@Operation(summary = "Get transaction", description = "Retrieve a single transaction by ID")
	@GetMapping("/transactions/{id}")
	@ResponseStatus(HttpStatus.OK)
	public TransactionResponse getTransaction(@PathVariable UUID id) {
		Transaction tx = transactionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
		return toResponse(tx);
	}

	private static TransactionResponse toResponse(Transaction tx) {
		return new TransactionResponse(
				tx.getId(),
				tx.getWalletId(),
				tx.getMoney(),
				tx.getDirection(),
				tx.getGroupType(),
				tx.getReferenceType(),
				tx.getReferenceId(),
				tx.getDescription(),
				tx.getOccurredAt()
		);
	}
}
