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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistence for the {@code dns_phonebook} table.
 */
public final class dns_phonebook_repository {
	public record phonebook_record(
			long record_id,
			String zone,
			String record_name,
			String record_type,
			String target,
			Integer port,
			int ttl_seconds) {
	}

	private final Connection connection;

	public dns_phonebook_repository(Connection connection) {
		this.connection = connection;
	}

	public void upsert(
			String zone,
			String record_name,
			String record_type,
			String target,
			Integer port,
			int ttl_seconds) throws SQLException {
		String sql = """
				INSERT INTO dns_phonebook (zone, record_name, record_type, target, port, ttl_seconds)
				VALUES (?, ?, ?, ?, ?, ?)
				ON CONFLICT(zone, record_name) DO UPDATE SET
				    record_type = excluded.record_type,
				    target = excluded.target,
				    port = excluded.port,
				    ttl_seconds = excluded.ttl_seconds
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, zone);
			statement.setString(2, record_name);
			statement.setString(3, record_type);
			statement.setString(4, target);
			if (port == null) {
				statement.setNull(5, java.sql.Types.INTEGER);
			} else {
				statement.setInt(5, port);
			}
			statement.setInt(6, ttl_seconds);
			statement.executeUpdate();
		}
	}

	public Optional<phonebook_record> find(String zone, String record_name) throws SQLException {
		String sql = """
				SELECT record_id, zone, record_name, record_type, target, port, ttl_seconds
				FROM dns_phonebook WHERE zone = ? AND record_name = ?
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, zone);
			statement.setString(2, record_name);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					return Optional.empty();
				}
				return Optional.of(map_row(result));
			}
		}
	}

	public List<phonebook_record> list_zone(String zone) throws SQLException {
		String sql = """
				SELECT record_id, zone, record_name, record_type, target, port, ttl_seconds
				FROM dns_phonebook WHERE zone = ? ORDER BY record_name
				""";
		List<phonebook_record> records = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, zone);
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					records.add(map_row(result));
				}
			}
		}
		return records;
	}

	private static phonebook_record map_row(ResultSet result) throws SQLException {
		int port = result.getInt("port");
		return new phonebook_record(
				result.getLong("record_id"),
				result.getString("zone"),
				result.getString("record_name"),
				result.getString("record_type"),
				result.getString("target"),
				result.wasNull() ? null : port,
				result.getInt("ttl_seconds"));
	}
}
