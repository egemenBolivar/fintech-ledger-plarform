package com.ekup.fintech.shared.api;

import java.net.URI;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.ekup.fintech.shared.exception.AccessDeniedException;
import com.ekup.fintech.shared.exception.CurrencyMismatchException;
import com.ekup.fintech.shared.exception.DomainException;
import com.ekup.fintech.shared.exception.DuplicateResourceException;
import com.ekup.fintech.shared.exception.DuplicateWalletException;
import com.ekup.fintech.shared.exception.IdempotencyConflictException;
import com.ekup.fintech.shared.exception.InsufficientBalanceException;
import com.ekup.fintech.shared.exception.InvalidAmountException;
import com.ekup.fintech.shared.exception.InvalidCredentialsException;
import com.ekup.fintech.shared.exception.ResourceNotFoundException;
import com.ekup.fintech.shared.exception.SameWalletTransferException;
import com.ekup.fintech.shared.exception.WalletClosedException;
import com.ekup.fintech.shared.exception.WalletSuspendedException;

/**
 * Global exception handler using RFC 7807 Problem Details format.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// ==================== 404 NOT FOUND ====================
	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
		log.warn("Resource not found: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND");
	}

	// ==================== 400 BAD REQUEST ====================
	@ExceptionHandler(InvalidAmountException.class)
	public ProblemDetail handleInvalidAmount(InvalidAmountException ex) {
		log.warn("Invalid amount: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_AMOUNT");
	}

	@ExceptionHandler(CurrencyMismatchException.class)
	public ProblemDetail handleCurrencyMismatch(CurrencyMismatchException ex) {
		log.warn("Currency mismatch: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "CURRENCY_MISMATCH");
	}

	@ExceptionHandler(SameWalletTransferException.class)
	public ProblemDetail handleSameWalletTransfer(SameWalletTransferException ex) {
		log.warn("Same wallet transfer: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "SAME_WALLET_TRANSFER");
	}

	// ==================== 409 CONFLICT ====================
	@ExceptionHandler(DuplicateWalletException.class)
	public ProblemDetail handleDuplicateWallet(DuplicateWalletException ex) {
		log.warn("Duplicate wallet: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), "DUPLICATE_WALLET");
	}

	@ExceptionHandler(IdempotencyConflictException.class)
	public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
		log.warn("Idempotency conflict: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), "IDEMPOTENCY_CONFLICT");
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ProblemDetail handleDuplicateResource(DuplicateResourceException ex) {
		log.warn("Duplicate resource: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), "DUPLICATE_RESOURCE");
	}

	// ==================== 401 UNAUTHORIZED ====================
	@ExceptionHandler(InvalidCredentialsException.class)
	public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
		log.warn("Invalid credentials: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_CREDENTIALS");
	}

	// ==================== 422 UNPROCESSABLE ENTITY ====================
	@ExceptionHandler(InsufficientBalanceException.class)
	public ProblemDetail handleInsufficientBalance(InsufficientBalanceException ex) {
		log.warn("Insufficient balance: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "INSUFFICIENT_BALANCE");
	}

	// ==================== 403 FORBIDDEN ====================
	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		log.warn("Access denied: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage(), "ACCESS_DENIED");
	}

	@ExceptionHandler(WalletSuspendedException.class)
	public ProblemDetail handleWalletSuspended(WalletSuspendedException ex) {
		log.warn("Wallet suspended: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage(), "WALLET_SUSPENDED");
	}

	@ExceptionHandler(WalletClosedException.class)
	public ProblemDetail handleWalletClosed(WalletClosedException ex) {
		log.warn("Wallet closed: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.FORBIDDEN, ex.getMessage(), "WALLET_CLOSED");
	}

	// ==================== General Domain Exception ====================
	@ExceptionHandler(DomainException.class)
	public ProblemDetail handleDomainException(DomainException ex) {
		log.error("Domain exception: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "DOMAIN_ERROR");
	}

	// ==================== Validation Errors ====================
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
		String errors = ex.getBindingResult().getFieldErrors()
				.stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Validation failed");
		log.warn("Validation errors: {}", errors);
		ProblemDetail problem = createProblemDetail(HttpStatus.BAD_REQUEST, errors, "VALIDATION_ERROR");
		problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.toList());
		return problem;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
		log.warn("Type mismatch: {}", message);
		return createProblemDetail(HttpStatus.BAD_REQUEST, message, "TYPE_MISMATCH");
	}

	// ==================== 409 CONFLICT (Optimistic Locking) ====================
	@ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
	public ProblemDetail handleOptimisticLocking(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
		log.warn("Optimistic locking failure: {}", ex.getMessage());
		return createProblemDetail(HttpStatus.CONFLICT, "Resource was modified by another request. Please retry.", "OPTIMISTIC_LOCK");
	}

	// ==================== Helper ====================
	private ProblemDetail createProblemDetail(HttpStatus status, String detail, String errorType) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create("https://api.fintech.ekup.com/errors/" + errorType));
		problem.setProperty("timestamp", Instant.now());
		problem.setProperty("errorCode", errorType);
		return problem;
	}
}
