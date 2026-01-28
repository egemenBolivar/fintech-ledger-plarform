package com.ekup.fintech.shared.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ekup.fintech.shared.exception.DomainException;
import com.ekup.fintech.shared.exception.IdempotencyConflictException;
import com.ekup.fintech.shared.exception.ResourceNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(IdempotencyConflictException.class)
	public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		pd.setProperty("timestamp", Instant.now());
		pd.setProperty("type", "IDEMPOTENCY_CONFLICT");
		return pd;
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		pd.setProperty("timestamp", Instant.now());
		pd.setProperty("type", "NOT_FOUND");
		return pd;
	}

	@ExceptionHandler(DomainException.class)
	public ProblemDetail handleDomain(DomainException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		pd.setProperty("timestamp", Instant.now());
		pd.setProperty("type", "DOMAIN_ERROR");
		return pd;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setDetail("Validation failed");
		pd.setProperty("timestamp", Instant.now());
		pd.setProperty("type", "VALIDATION_ERROR");
		return pd;
	}
}
