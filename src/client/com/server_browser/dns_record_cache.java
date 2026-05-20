/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches DNS lookup results with TTL-based expiration.
 */
public final class dns_record_cache {
	public record cached_dns_record(String hostname, InetAddress[] addresses, Instant expires_at) {
		public boolean is_expired(Instant now) {
			return !expires_at.isAfter(now);
		}
	}

	private final Duration default_ttl;
	private final Map<String, cached_dns_record> records = new ConcurrentHashMap<>();

	public dns_record_cache() {
		this(Duration.ofMinutes(5));
	}

	public dns_record_cache(Duration default_ttl) {
		this.default_ttl = default_ttl;
	}

	public Optional<cached_dns_record> get(String hostname) {
		if (hostname == null || hostname.isBlank()) {
			return Optional.empty();
		}
		cached_dns_record record = records.get(normalize(hostname));
		if (record == null) {
			return Optional.empty();
		}
		if (record.is_expired(Instant.now())) {
			records.remove(normalize(hostname));
			return Optional.empty();
		}
		return Optional.of(record);
	}

	public void put(String hostname, InetAddress[] addresses) {
		put(hostname, addresses, default_ttl);
	}

	public void put(String hostname, InetAddress[] addresses, Duration ttl) {
		if (hostname == null || hostname.isBlank() || addresses == null || addresses.length == 0) {
			return;
		}
		records.put(
				normalize(hostname),
				new cached_dns_record(
						normalize(hostname),
						addresses.clone(),
						Instant.now().plus(ttl)));
	}

	public void invalidate(String hostname) {
		if (hostname != null) {
			records.remove(normalize(hostname));
		}
	}

	public void purge_expired() {
		Instant now = Instant.now();
		records.entrySet().removeIf(entry -> entry.getValue().is_expired(now));
	}

	private static String normalize(String hostname) {
		return hostname.trim().toLowerCase();
	}
}
