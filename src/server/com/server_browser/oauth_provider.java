/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

/**
 * Supported corporate identity providers.
 */
public enum oauth_provider {
	GOOGLE("google"),
	MICROSOFT("microsoft"),
	APPLE("apple");

	private final String id;

	oauth_provider(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static oauth_provider from_id(String value) {
		if (value == null) {
			throw new IllegalArgumentException("provider required");
		}
		String normalized = value.trim().toLowerCase();
		for (oauth_provider provider : values()) {
			if (provider.id.equals(normalized)) {
				return provider;
			}
		}
		throw new IllegalArgumentException("Unknown OAuth provider: " + value);
	}
}
