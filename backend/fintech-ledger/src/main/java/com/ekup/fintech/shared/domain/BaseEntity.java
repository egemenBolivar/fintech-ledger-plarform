package com.ekup.fintech.shared.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

@MappedSuperclass
public abstract class BaseEntity {
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public Instant getCreatedAt() {
		return createdAt;
	}

	@PrePersist
	void onCreate() {
		if (this.createdAt == null) {
			this.createdAt = Instant.now();
		}
	}
}
