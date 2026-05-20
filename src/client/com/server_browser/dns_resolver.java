/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.InetAddress;
import java.util.Optional;

/**
 * DNS resolver facade: lookup, cache, and retry.
 */
public final class dns_resolver {
	private final dns_hostname_lookup hostname_lookup;
	private final dns_record_cache record_cache;
	private final dns_query_retry query_retry;

	public dns_resolver() {
		this(new dns_hostname_lookup(), new dns_record_cache(), new dns_query_retry());
	}

	public dns_resolver(
			dns_hostname_lookup hostname_lookup,
			dns_record_cache record_cache,
			dns_query_retry query_retry) {
		this.hostname_lookup = hostname_lookup;
		this.record_cache = record_cache;
		this.query_retry = query_retry;
	}

	public dns_hostname_lookup lookup() {
		return hostname_lookup;
	}

	public dns_record_cache cache() {
		return record_cache;
	}

	public dns_query_retry retry() {
		return query_retry;
	}

	public Optional<InetAddress> resolve_first(String hostname) {
		Optional<InetAddress[]> all = resolve_all(hostname);
		return all.map(addresses -> addresses[0]);
	}

	public Optional<InetAddress[]> resolve_all(String hostname) {
		if (hostname == null || hostname.isBlank()) {
			return Optional.empty();
		}
		Optional<dns_record_cache.cached_dns_record> cached = record_cache.get(hostname);
		if (cached.isPresent()) {
			return Optional.of(cached.get().addresses());
		}
		try {
			InetAddress[] addresses = query_retry.execute(() -> hostname_lookup.lookup(hostname));
			record_cache.put(hostname, addresses);
			return Optional.of(addresses);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public void invalidate(String hostname) {
		record_cache.invalidate(hostname);
	}
}
