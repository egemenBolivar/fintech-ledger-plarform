package com.ekup.fintech.ledger.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ekup.fintech.ledger.domain.ReferenceType;
import com.ekup.fintech.ledger.domain.Transaction;
import com.ekup.fintech.ledger.domain.TransactionDirection;
import com.ekup.fintech.ledger.domain.TransactionGroupType;
import com.ekup.fintech.shared.domain.Currency;

public interface TransactionJpaRepository extends JpaRepository<Transaction, UUID> {
	Page<Transaction> findByWalletId(UUID walletId, Pageable pageable);

	@Query(
			"select t from Transaction t " +
			"where t.walletId = :walletId " +
			"and (:direction is null or t.direction = :direction) " +
			"and (:groupType is null or t.groupType = :groupType) " +
			"and (:referenceType is null or t.referenceType = :referenceType) " +
			"and (cast(:from as timestamp) is null or t.occurredAt >= :from) " +
			"and (cast(:to as timestamp) is null or t.occurredAt <= :to)"
	)
	Page<Transaction> searchByWalletId(
			@Param("walletId") UUID walletId,
			@Param("direction") TransactionDirection direction,
			@Param("groupType") TransactionGroupType groupType,
			@Param("referenceType") ReferenceType referenceType,
			@Param("from") Instant from,
			@Param("to") Instant to,
			Pageable pageable
	);

	@Query(
			"select coalesce(sum(case when t.direction = com.ekup.fintech.ledger.domain.TransactionDirection.CREDIT then t.amount else -t.amount end), 0) " +
			"from Transaction t " +
			"where t.walletId = :walletId and t.currency = :currency"
	)
	BigDecimal sumSignedAmount(@Param("walletId") UUID walletId, @Param("currency") Currency currency);
}
