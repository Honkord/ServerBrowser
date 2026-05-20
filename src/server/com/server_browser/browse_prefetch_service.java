/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;

/**
 * Prioritized parallel prefetch of browse subresources into the shared cache.
 */
public final class browse_prefetch_service {
	private static final int MAX_PARALLEL = 20;
	private static final int MAX_PER_PAGE = 80;

	private final fast_page_fetcher fetcher;
	private final browse_resource_cache cache;
	private final Semaphore parallel_limit = new Semaphore(MAX_PARALLEL);

	public browse_prefetch_service(fast_page_fetcher fetcher, browse_resource_cache cache) {
		this.fetcher = fetcher;
		this.cache = cache;
	}

	public void prefetch_html(byte[] html, String base_url) {
		prefetch(browse_resource_catalog.from_html(html, base_url));
	}

	public void prefetch_css(String css, String base_url) {
		prefetch(browse_resource_catalog.from_css(css, base_url, browse_fetch_priority.NORMAL, 0));
	}

	public void prefetch(List<browse_resource_catalog.catalog_entry> entries) {
		if (entries == null || entries.isEmpty()) {
			return;
		}
		List<browse_resource_catalog.catalog_entry> queue = entries.stream()
				.filter(entry -> browse_allowed_target.is_allowed(entry.url()))
				.filter(entry -> !cache.contains(entry.url()))
				.sorted(Comparator
						.comparing((browse_resource_catalog.catalog_entry entry) -> entry.priority().rank())
						.thenComparingInt(browse_resource_catalog.catalog_entry::order))
				.limit(MAX_PER_PAGE)
				.toList();
		for (browse_resource_catalog.catalog_entry entry : queue) {
			fetcher.executor().execute(() -> fetch_entry(entry));
		}
	}

	private void fetch_entry(browse_resource_catalog.catalog_entry entry) {
		if (cache.contains(entry.url())) {
			return;
		}
		if (!cache.reserve(entry.url())) {
			return;
		}
		boolean acquired = false;
		try {
			parallel_limit.acquire();
			acquired = true;
			if (cache.contains(entry.url())) {
				return;
			}
			Optional<fast_page_fetcher.fetch_result> result =
					fetcher.fetch(URI.create(entry.url()), entry.kind());
			if (result.isPresent()) {
				cache.put(entry.url(), result.get());
			} else {
				cache.release_in_flight(entry.url());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			cache.release_in_flight(entry.url());
		} finally {
			if (acquired) {
				parallel_limit.release();
			}
		}
	}
}
