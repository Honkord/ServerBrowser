/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves redirect targets from HTTP response status and Location headers.
 */
public final class http_redirect_processing {
	private final int max_redirects;

	public http_redirect_processing() {
		this(10);
	}

	public http_redirect_processing(int max_redirects) {
		if (max_redirects < 0) {
			throw new IllegalArgumentException("max_redirects must be non-negative");
		}
		this.max_redirects = max_redirects;
	}

	public boolean is_redirect(int status_code) {
		return status_code == 301
				|| status_code == 302
				|| status_code == 303
				|| status_code == 307
				|| status_code == 308;
	}

	public Optional<URI> resolve_redirect(int status_code, Map<String, List<String>> headers, URI current_uri) {
		if (!is_redirect(status_code) || headers == null || current_uri == null) {
			return Optional.empty();
		}
		http_header_processing header_processing = new http_header_processing();
		Optional<String> location = header_processing.first_value(headers, "Location");
		if (location.isEmpty() || location.get().isBlank()) {
			return Optional.empty();
		}
		return Optional.of(current_uri.resolve(location.get().trim()));
	}

	public boolean should_follow(int redirects_followed) {
		return redirects_followed < max_redirects;
	}

	public int max_redirects() {
		return max_redirects;
	}
}
