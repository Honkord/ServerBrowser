/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/**
 * Links corporate OAuth subjects to local user accounts.
 */
public final class oauth_identity_repository {
	public record oauth_identity_row(
			String provider,
			String provider_subject,
			String user_id,
			String email,
			Instant created_at) {
	}

	private final Connection connection;

	public oauth_identity_repository(Connection connection) {
		this.connection = connection;
	}

	public void insert(oauth_identity_row row) throws SQLException {
		String sql = """
				INSERT INTO oauth_identities (provider, provider_subject, user_id, email, created_at)
				VALUES (?, ?, ?, ?, ?)
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, row.provider());
			statement.setString(2, row.provider_subject());
			statement.setString(3, row.user_id());
			statement.setString(4, row.email());
			statement.setString(5, row.created_at().toString());
			statement.executeUpdate();
		}
	}

	public Optional<oauth_identity_row> find(String provider, String provider_subject) throws SQLException {
		String sql = """
				SELECT provider, provider_subject, user_id, email, created_at
				FROM oauth_identities
				WHERE provider = ? AND provider_subject = ?
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, provider);
			statement.setString(2, provider_subject);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					return Optional.empty();
				}
				return Optional.of(new oauth_identity_row(
						result.getString("provider"),
						result.getString("provider_subject"),
						result.getString("user_id"),
						result.getString("email"),
						Instant.parse(result.getString("created_at"))));
			}
		}
	}
}
