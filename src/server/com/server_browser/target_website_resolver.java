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
 * Determines the remote website (host, port, path) from an inbound proxy request URI.
 */
public final class target_website_resolver {
	public record route_target(String host, int port, String path) {
	}

	public Optional<route_target> resolve_target(String request_uri) {
		if (request_uri == null || request_uri.isBlank()) {
			return Optional.empty();
		}
		try {
			URI uri = URI.create(request_uri.trim());
			String host = uri.getHost();
			if (host == null || host.isBlank()) {
				return Optional.empty();
			}
			int port = uri.getPort();
			if (port < 0) {
				port = default_port(uri.getScheme());
			}
			String path = uri.getRawPath();
			if (path == null || path.isEmpty()) {
				path = "/";
			}
			String query = uri.getRawQuery();
			if (query != null && !query.isEmpty()) {
				path = path + "?" + query;
			}
			return Optional.of(new route_target(host.toLowerCase(), port, path));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static int default_port(String scheme) {
		if (scheme != null && scheme.equalsIgnoreCase("http")) {
			return 80;
		}
		return 443;
	}
}
