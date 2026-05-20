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
 * Persistence for {@code cache_indexes} table rows.
 */
public final class cache_index_repository {
	public record cache_index_row(long cache_id, String cache_key, String cache_type, String payload_ref, Instant expires_at) {
	}

	private final Connection connection;

	public cache_index_repository(Connection connection) {
		this.connection = connection;
	}

	public void upsert(String cache_key, String cache_type, String payload_ref, Instant expires_at) throws SQLException {
		String sql = """
				INSERT INTO cache_indexes (cache_key, cache_type, payload_ref, expires_at)
				VALUES (?, ?, ?, ?)
				ON CONFLICT(cache_key) DO UPDATE SET
				    cache_type = excluded.cache_type,
				    payload_ref = excluded.payload_ref,
				    expires_at = excluded.expires_at
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, cache_key);
			statement.setString(2, cache_type);
			statement.setString(3, payload_ref);
			statement.setString(4, expires_at.toString());
			statement.executeUpdate();
		}
	}

	public Optional<cache_index_row> find_valid(String cache_key, Instant now) throws SQLException {
		String sql = """
				SELECT cache_id, cache_key, cache_type, payload_ref, expires_at
				FROM cache_indexes
				WHERE cache_key = ? AND expires_at > ?
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, cache_key);
			statement.setString(2, now.toString());
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					return Optional.empty();
				}
				return Optional.of(new cache_index_row(
						result.getLong("cache_id"),
						result.getString("cache_key"),
						result.getString("cache_type"),
						result.getString("payload_ref"),
						Instant.parse(result.getString("expires_at"))));
			}
		}
	}

	public void delete_expired(Instant now) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"DELETE FROM cache_indexes WHERE expires_at <= ?")) {
			statement.setString(1, now.toString());
			statement.executeUpdate();
		}
	}
}
