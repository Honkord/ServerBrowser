/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.URI;

/**
 * Validates outbound browse and proxy target URLs.
 */
public final class browse_allowed_target {
	private browse_allowed_target() {
	}

	public static boolean is_allowed(String url) {
		if (url == null || url.isBlank()) {
			return false;
		}
		try {
			URI uri = URI.create(url.trim());
			String scheme = uri.getScheme();
			if (scheme == null) {
				return false;
			}
			String lower = scheme.toLowerCase();
			return ("http".equals(lower) || "https".equals(lower)) && uri.getHost() != null;
		} catch (Exception e) {
			return false;
		}
	}
}
