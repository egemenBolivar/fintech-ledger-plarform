package com.ekup.fintech.ledger.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecord, UUID> {
	Optional<IdempotencyRecord> findByOperationAndIdempotencyKey(String operation, UUID idempotencyKey);
}
