/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.InetAddress;
import java.util.function.Supplier;

/**
 * Retries failed DNS queries with bounded attempts and backoff.
 */
public final class dns_query_retry {
	@FunctionalInterface
	public interface dns_query<T> {
		T execute() throws Exception;
	}

	private final int max_attempts;
	private final long backoff_ms;

	public dns_query_retry() {
		this(3, 100);
	}

	public dns_query_retry(int max_attempts, long backoff_ms) {
		if (max_attempts < 1) {
			throw new IllegalArgumentException("max_attempts must be at least 1");
		}
		this.max_attempts = max_attempts;
		this.backoff_ms = backoff_ms;
	}

	public <T> T execute(dns_query<T> query) throws Exception {
		Exception last_failure = null;
		for (int attempt = 1; attempt <= max_attempts; attempt++) {
			try {
				return query.execute();
			} catch (Exception e) {
				last_failure = e;
				if (attempt < max_attempts) {
					Thread.sleep(backoff_ms * attempt);
				}
			}
		}
		throw last_failure;
	}

	public InetAddress[] execute_lookup(Supplier<InetAddress[]> lookup) throws Exception {
		return execute(lookup::get);
	}
}
