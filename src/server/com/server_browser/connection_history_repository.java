/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Persistence for {@code connection_history} table rows.
 */
public final class connection_history_repository {
	private final Connection connection;

	public connection_history_repository(Connection connection) {
		this.connection = connection;
	}

	public long insert_started(
			String user_id,
			String remote_host,
			int remote_port,
			String protocol) throws SQLException {
		String sql = """
				INSERT INTO connection_history
				(user_id, remote_host, remote_port, protocol, bytes_streamed, started_at)
				VALUES (?, ?, ?, ?, 0, ?)
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, user_id);
			statement.setString(2, remote_host);
			statement.setInt(3, remote_port);
			statement.setString(4, protocol);
			statement.setString(5, Instant.now().toString());
			statement.executeUpdate();
			try (var keys = statement.getGeneratedKeys()) {
				if (keys.next()) {
					return keys.getLong(1);
				}
			}
		}
		return -1;
	}

	public void complete(long history_id, long bytes_streamed) throws SQLException {
		String sql = """
				UPDATE connection_history
				SET bytes_streamed = ?, ended_at = ?
				WHERE history_id = ?
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setLong(1, bytes_streamed);
			statement.setString(2, Instant.now().toString());
			statement.setLong(3, history_id);
			statement.executeUpdate();
		}
	}
}
