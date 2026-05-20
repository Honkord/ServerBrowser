/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.URI;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Coordinates OAuth sign-in with the local user database.
 */
public final class oauth_service {
	private final sql_database database;
	private final authentication_subsystem authentication;
	private final oauth_client client = new oauth_client();

	public oauth_service(sql_database database, authentication_subsystem authentication) {
		this.database = database;
		this.authentication = authentication;
	}

	public URI start_sign_in(oauth_provider provider) throws SQLException {
		oauth_config.provider_settings settings = oauth_config.for_provider(provider);
		if (!settings.enabled()) {
			throw new IllegalStateException(
					provider.id() + " sign-in is not configured. Edit "
							+ server_configuration.get().oauth_config_file()
							+ " (see oauth.properties.example).");
		}
		database.oauth_states().delete_expired(Instant.now());
		oauth_client.pkce_pair pkce = client.create_pkce();
		database.oauth_states().insert(new oauth_state_repository.oauth_state_row(
				pkce.state(),
				provider.id(),
				pkce.code_verifier(),
				Instant.now().plus(Duration.ofMinutes(10))));
		return client.authorization_url(provider, pkce);
	}

	public session_manager.session_record complete_sign_in(
			oauth_provider provider,
			String code,
			String state)
			throws Exception {
		Optional<oauth_state_repository.oauth_state_row> pending = database.oauth_states().consume(state);
		if (pending.isEmpty() || !pending.get().provider().equals(provider.id())) {
			throw new IllegalStateException("Invalid or expired OAuth state");
		}
		oauth_client.oauth_user user = client.exchange_code(provider, code, pending.get().code_verifier());
		return authentication.login_with_oauth(provider, user);
	}
}
