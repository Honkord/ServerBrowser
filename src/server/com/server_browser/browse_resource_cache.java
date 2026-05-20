/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache of fetched remote resources keyed by absolute URL.
 */
public final class browse_resource_cache {
	private static final int MAX_ENTRIES = 768;
	private static final Duration TTL = Duration.ofMinutes(15);

	private final ConcurrentHashMap<String, cache_entry> entries = new ConcurrentHashMap<>();

	public Optional<fast_page_fetcher.fetch_result> get(String url) {
		if (url == null || url.isBlank()) {
			return Optional.empty();
		}
		cache_entry entry = entries.get(url);
		if (entry == null) {
			return Optional.empty();
		}
		if (entry.in_flight || entry.expires_at.isBefore(Instant.now())) {
			if (entry.in_flight) {
				return Optional.empty();
			}
			entries.remove(url, entry);
			return Optional.empty();
		}
		return Optional.of(entry.result);
	}

	public void put(String url, fast_page_fetcher.fetch_result result) {
		if (url == null || url.isBlank() || result == null) {
			return;
		}
		if (entries.size() >= MAX_ENTRIES) {
			evict_one();
		}
		entries.put(url, new cache_entry(result, Instant.now().plus(TTL), false));
	}

	public boolean contains(String url) {
		return get(url).isPresent();
	}

	/**
	 * Waits briefly for an in-flight prefetch to finish before giving up.
	 */
	public Optional<fast_page_fetcher.fetch_result> await(String url, Duration max_wait) {
		Instant deadline = Instant.now().plus(max_wait);
		while (Instant.now().isBefore(deadline)) {
			Optional<fast_page_fetcher.fetch_result> hit = get(url);
			if (hit.isPresent()) {
				return hit;
			}
			cache_entry entry = entries.get(url);
			if (entry == null || !entry.in_flight) {
				break;
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return get(url);
	}

	public boolean reserve(String url) {
		if (url == null || url.isBlank()) {
			return false;
		}
		return entries.putIfAbsent(url, cache_entry.pending()) == null;
	}

	public void release_in_flight(String url) {
		if (url == null) {
			return;
		}
		cache_entry entry = entries.get(url);
		if (entry != null && entry.in_flight) {
			entries.remove(url, entry);
		}
	}

	private void evict_one() {
		Instant now = Instant.now();
		for (var candidate : entries.entrySet()) {
			if (candidate.getValue().expires_at.isBefore(now) || candidate.getValue().in_flight) {
				entries.remove(candidate.getKey(), candidate.getValue());
				return;
			}
		}
		if (!entries.isEmpty()) {
			String key = entries.keys().nextElement();
			entries.remove(key);
		}
	}

	private record cache_entry(fast_page_fetcher.fetch_result result, Instant expires_at, boolean in_flight) {
		static cache_entry pending() {
			return new cache_entry(null, Instant.now().plus(Duration.ofMinutes(1)), true);
		}
	}
}
