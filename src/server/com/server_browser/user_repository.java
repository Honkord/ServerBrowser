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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistence for {@code users} table rows.
 */
public final class user_repository {
	public record user_row(
			String user_id,
			String username,
			String password_hash,
			byte[] salt,
			String email,
			String display_name,
			Instant created_at,
			String status) {
	}

	private final Connection connection;

	public user_repository(Connection connection) {
		this.connection = connection;
	}

	public void insert(user_row user) throws SQLException {
		String sql = """
				INSERT INTO users (user_id, username, password_hash, salt, email, display_name, created_at, status)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, user.user_id());
			statement.setString(2, user.username());
			statement.setString(3, user.password_hash());
			statement.setBytes(4, user.salt());
			statement.setString(5, user.email());
			statement.setString(6, user.display_name());
			statement.setString(7, user.created_at().toString());
			statement.setString(8, user.status());
			statement.executeUpdate();
		}
	}

	public Optional<user_row> find_by_username(String username) throws SQLException {
		String sql = """
				SELECT user_id, username, password_hash, salt, email, display_name, created_at, status
				FROM users WHERE username = ?
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, username);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					return Optional.empty();
				}
				return Optional.of(map_row(result));
			}
		}
	}

	public Optional<user_row> find_by_id(String user_id) throws SQLException {
		String sql = """
				SELECT user_id, username, password_hash, salt, email, display_name, created_at, status
				FROM users WHERE user_id = ?
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, user_id);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					return Optional.empty();
				}
				return Optional.of(map_row(result));
			}
		}
	}

	public List<String> list_user_ids() throws SQLException {
		List<String> ids = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement("SELECT user_id FROM users");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				ids.add(result.getString("user_id"));
			}
		}
		return ids;
	}

	public void update_status(String user_id, String status) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"UPDATE users SET status = ? WHERE user_id = ?")) {
			statement.setString(1, status);
			statement.setString(2, user_id);
			statement.executeUpdate();
		}
	}

	private static user_row map_row(ResultSet result) throws SQLException {
		return new user_row(
				result.getString("user_id"),
				result.getString("username"),
				result.getString("password_hash"),
				result.getBytes("salt"),
				result.getString("email"),
				result.getString("display_name"),
				Instant.parse(result.getString("created_at")),
				result.getString("status"));
	}
}
