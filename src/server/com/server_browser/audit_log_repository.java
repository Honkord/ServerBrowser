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
 * Persistence for {@code audit_logs} table rows.
 */
public final class audit_log_repository {
	private final Connection connection;

	public audit_log_repository(Connection connection) {
		this.connection = connection;
	}

	public void insert(String user_id, String event_type, String message) throws SQLException {
		String sql = """
				INSERT INTO audit_logs (user_id, event_type, message, created_at)
				VALUES (?, ?, ?, ?)
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, user_id);
			statement.setString(2, event_type);
			statement.setString(3, message);
			statement.setString(4, Instant.now().toString());
			statement.executeUpdate();
		}
	}
}
