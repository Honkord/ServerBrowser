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
 * Persistence for {@code api_keys} table rows.
 */
public final class api_key_repository {
	public record api_key_row(String key_id, String user_id, String api_key_hash, Instant expires_at) {
	}

	private final Connection connection;

	public api_key_repository(Connection connection) {
		this.connection = connection;
	}

	public void insert(api_key_row key) throws SQLException {
		String sql = """
				INSERT INTO api_keys (key_id, user_id, api_key_hash, expires_at)
				VALUES (?, ?, ?, ?)
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, key.key_id());
			statement.setString(2, key.user_id());
			statement.setString(3, key.api_key_hash());
			if (key.expires_at() == null) {
				statement.setString(4, null);
			} else {
				statement.setString(4, key.expires_at().toString());
			}
			statement.executeUpdate();
		}
	}

	public Optional<api_key_row> find_by_hash(String api_key_hash) throws SQLException {
		String sql = "SELECT key_id, user_id, api_key_hash, expires_at FROM api_keys WHERE api_key_hash = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, api_key_hash);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					return Optional.empty();
				}
				return Optional.of(map_row(result));
			}
		}
	}

	private static api_key_row map_row(ResultSet result) throws SQLException {
		String expires = result.getString("expires_at");
		return new api_key_row(
				result.getString("key_id"),
				result.getString("user_id"),
				result.getString("api_key_hash"),
				expires == null ? null : Instant.parse(expires));
	}
}
