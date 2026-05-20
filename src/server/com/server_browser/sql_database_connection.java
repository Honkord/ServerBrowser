/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens the SQLite database and applies the schema under {@code data/db/}.
 */
public final class sql_database_connection {
	public static final String JDBC_DRIVER = "org.sqlite.JDBC";

	private final Connection connection;

	private sql_database_connection(Connection connection) {
		this.connection = connection;
	}

	public Connection connection() {
		return connection;
	}

	public static sql_database_connection open() throws SQLException, IOException {
		return open(application_paths.database_schema(), application_paths.database_file());
	}

	public static sql_database_connection open(Path schema_path, Path database_path) throws SQLException, IOException {
		try {
			Class.forName(JDBC_DRIVER);
		} catch (ClassNotFoundException e) {
			throw new SQLException("SQLite JDBC driver not found. Add lib/sqlite-jdbc.jar to the classpath.", e);
		}
		Path parent = database_path.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database_path.toAbsolutePath());
		connection.setAutoCommit(true);
		apply_schema(connection, schema_path);
		apply_migrations(connection, schema_path.getParent().resolve("migrations"));
		return new sql_database_connection(connection);
	}

	private static void apply_migrations(Connection connection, Path migrations_dir) throws SQLException, IOException {
		if (!Files.isDirectory(migrations_dir)) {
			return;
		}
		try (Stream<Path> files = Files.list(migrations_dir)) {
			for (Path file : files.sorted().toList()) {
				if (!file.getFileName().toString().endsWith(".sql")) {
					continue;
				}
				execute_sql_script(connection, Files.readString(file), true);
			}
		}
	}

	private static void apply_schema(Connection connection, Path schema_path) throws SQLException, IOException {
		if (!Files.exists(schema_path)) {
			throw new IOException("Schema file not found: " + schema_path);
		}
		execute_sql_script(connection, Files.readString(schema_path), false);
	}

	private static void execute_sql_script(Connection connection, String sql, boolean ignore_errors) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			for (String part : sql.split(";")) {
				String statement_sql = strip_sql_comments(part).trim();
				if (statement_sql.isEmpty()) {
					continue;
				}
				try {
					statement.execute(statement_sql);
				} catch (SQLException error) {
					if (!ignore_errors) {
						throw error;
					}
				}
			}
		}
	}

	/** Removes line comments so a leading {@code --} block comment does not skip following SQL. */
	private static String strip_sql_comments(String sql) {
		StringBuilder cleaned = new StringBuilder(sql.length());
		for (String line : sql.split("\n")) {
			int comment = line.indexOf("--");
			String without_comment = comment >= 0 ? line.substring(0, comment) : line;
			cleaned.append(without_comment.trim()).append('\n');
		}
		return cleaned.toString();
	}

	public void close() throws SQLException {
		if (connection != null && !connection.isClosed()) {
			connection.close();
		}
	}
}
