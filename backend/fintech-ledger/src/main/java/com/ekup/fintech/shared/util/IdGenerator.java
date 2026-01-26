package com.ekup.fintech.shared.util;

import java.util.UUID;

public final class IdGenerator {
	private IdGenerator() {
	}

	public static UUID newId() {
		return UUID.randomUUID();
	}
}
