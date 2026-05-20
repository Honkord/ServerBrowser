/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

/**
 * OAuth client settings from {@code data/config/oauth.properties}.
 */
public final class oauth_config {
	public record provider_settings(
			String client_id,
			String client_secret,
			String redirect_uri,
			String tenant) {
		public boolean enabled() {
			return client_id != null && !client_id.isBlank() && redirect_uri != null && !redirect_uri.isBlank();
		}
	}

	private oauth_config() {
	}

	public static provider_settings google() {
		return settings("google", "");
	}

	public static provider_settings microsoft() {
		return settings("microsoft", server_configuration.oauth("oauth.microsoft.tenant", "common"));
	}

	public static provider_settings apple() {
		return settings("apple", server_configuration.oauth("oauth.apple.team_id", ""));
	}

	public static provider_settings for_provider(oauth_provider provider) {
		return switch (provider) {
			case GOOGLE -> google();
			case MICROSOFT -> microsoft();
			case APPLE -> apple();
		};
	}

	private static provider_settings settings(String provider_id, String tenant) {
		String prefix = "oauth." + provider_id + ".";
		oauth_provider provider = oauth_provider.from_id(provider_id);
		return new provider_settings(
				server_configuration.oauth(prefix + "client_id", server_configuration.get(prefix + "client_id", "")),
				server_configuration.oauth(prefix + "client_secret", server_configuration.get(prefix + "client_secret", "")),
				server_configuration.get().oauth_redirect_uri(provider),
				tenant);
	}
}
