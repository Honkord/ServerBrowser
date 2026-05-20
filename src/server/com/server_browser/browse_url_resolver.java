/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.URI;
import java.util.Optional;

/**
 * Resolves relative references against a document base URL.
 */
public final class browse_url_resolver {
	private browse_url_resolver() {
	}

	public static Optional<String> resolve_reference(String reference, URI base) {
		if (reference == null || base == null) {
			return Optional.empty();
		}
		String trimmed = reference.trim();
		if (trimmed.isEmpty()
				|| trimmed.startsWith("#")
				|| trimmed.startsWith("data:")
				|| trimmed.startsWith("blob:")
				|| trimmed.startsWith("javascript:")
				|| trimmed.startsWith("mailto:")
				|| trimmed.startsWith("about:")) {
			return Optional.empty();
		}
		try {
			if (trimmed.startsWith("//")) {
				trimmed = base.getScheme() + ":" + trimmed;
			}
			return Optional.of(base.resolve(trimmed).toString());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public static URI parse_base(String base_url) {
		if (base_url == null || base_url.isBlank()) {
			return null;
		}
		try {
			return URI.create(base_url.trim());
		} catch (Exception e) {
			return null;
		}
	}
}
