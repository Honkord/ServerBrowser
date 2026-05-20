/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

/**
 * Selects outbound protocol (HTTP or HTTPS) for a resolved target.
 */
public final class protocol_selector {
	public enum protocol {
		HTTP,
		HTTPS
	}

	public protocol select(target_website_resolver.route_target target) {
		if (target == null) {
			throw new IllegalArgumentException("target is required");
		}
		if (target.port() == 80) {
			return protocol.HTTP;
		}
		if (target.port() == 443) {
			return protocol.HTTPS;
		}
		// Non-standard ports: treat high ports as HTTPS by default for outbound fetch.
		return target.port() > 1024 ? protocol.HTTPS : protocol.HTTP;
	}

	public protocol select_for_scheme(String scheme) {
		if (scheme != null && scheme.equalsIgnoreCase("http")) {
			return protocol.HTTP;
		}
		return protocol.HTTPS;
	}
}
