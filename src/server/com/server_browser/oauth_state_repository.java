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
 * Temporary OAuth PKCE state storage.
 */
public final class oauth_state_repository {
	public record oauth_state_row(String state, String provider, String code_verifier, Instant expires_at) {
	}

	private final Connection connection;

	public oauth_state_repository(Connection connection) {
		this.connection = connection;
	}

	public void insert(oauth_state_row row) throws SQLException {
		String sql = """
				INSERT INTO oauth_states (state, provider, code_verifier, expires_at)
				VALUES (?, ?, ?, ?)
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, row.state());
			statement.setString(2, row.provider());
			statement.setString(3, row.code_verifier());
			statement.setString(4, row.expires_at().toString());
			statement.executeUpdate();
		}
	}

	public Optional<oauth_state_row> consume(String state) throws SQLException {
		String sql = """
				SELECT state, provider, code_verifier, expires_at
				FROM oauth_states
				WHERE state = ?
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, state);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					return Optional.empty();
				}
				oauth_state_row row = new oauth_state_row(
						result.getString("state"),
						result.getString("provider"),
						result.getString("code_verifier"),
						Instant.parse(result.getString("expires_at")));
				delete(state);
				if (row.expires_at().isBefore(Instant.now())) {
					return Optional.empty();
				}
				return Optional.of(row);
			}
		}
	}

	public void delete_expired(Instant now) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"DELETE FROM oauth_states WHERE expires_at < ?")) {
			statement.setString(1, now.toString());
			statement.executeUpdate();
		}
	}

	private void delete(String state) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM oauth_states WHERE state = ?")) {
			statement.setString(1, state);
			statement.executeUpdate();
		}
	}
}
