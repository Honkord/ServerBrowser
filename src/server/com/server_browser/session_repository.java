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
 * Persistence for {@code sessions} table rows.
 */
public final class session_repository {
	private final Connection connection;

	public session_repository(Connection connection) {
		this.connection = connection;
	}

	public void insert(session_manager.session_record session) throws SQLException {
		String sql = """
				INSERT INTO sessions (session_id, user_id, access_token, refresh_token, expires_at)
				VALUES (?, ?, ?, ?, ?)
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, session.session_id());
			statement.setString(2, session.user_id());
			statement.setString(3, session.access_token());
			statement.setString(4, session.refresh_token());
			statement.setString(5, session.expires_at().toString());
			statement.executeUpdate();
		}
	}

	public Optional<session_manager.session_record> find_by_access_token(String access_token) throws SQLException {
		String sql = """
				SELECT session_id, user_id, access_token, refresh_token, expires_at
				FROM sessions WHERE access_token = ?
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, access_token);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					return Optional.empty();
				}
				return Optional.of(map_row(result));
			}
		}
	}

	public boolean delete_by_session_id(String session_id) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"DELETE FROM sessions WHERE session_id = ?")) {
			statement.setString(1, session_id);
			return statement.executeUpdate() > 0;
		}
	}

	public void delete_expired(Instant now) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"DELETE FROM sessions WHERE expires_at <= ?")) {
			statement.setString(1, now.toString());
			statement.executeUpdate();
		}
	}

	private static session_manager.session_record map_row(ResultSet result) throws SQLException {
		return new session_manager.session_record(
				result.getString("session_id"),
				result.getString("user_id"),
				result.getString("access_token"),
				result.getString("refresh_token"),
				Instant.parse(result.getString("expires_at")));
	}
}
