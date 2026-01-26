package com.ekup.fintech.shared.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ekup.fintech.shared.exception.DomainException;

@RestControllerAdvice
public class ApiExceptionHandler {
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
