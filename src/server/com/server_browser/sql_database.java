/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Database facade exposing all SQL repositories.
 */
public final class sql_database implements AutoCloseable {
	private final sql_database_connection database_connection;
	private final user_repository users;
	private final api_key_repository api_keys;
	private final session_repository sessions;
	private final audit_log_repository audit_logs;
	private final connection_history_repository connection_history;
	private final cache_index_repository cache_indexes;
	private final dns_phonebook_repository dns_phonebook;
	private final oauth_identity_repository oauth_identities;
	private final oauth_state_repository oauth_states;

	private sql_database(
			sql_database_connection database_connection,
			user_repository users,
			api_key_repository api_keys,
			session_repository sessions,
			audit_log_repository audit_logs,
			connection_history_repository connection_history,
			cache_index_repository cache_indexes,
			dns_phonebook_repository dns_phonebook,
			oauth_identity_repository oauth_identities,
			oauth_state_repository oauth_states) {
		this.database_connection = database_connection;
		this.users = users;
		this.api_keys = api_keys;
		this.sessions = sessions;
		this.audit_logs = audit_logs;
		this.connection_history = connection_history;
		this.cache_indexes = cache_indexes;
		this.dns_phonebook = dns_phonebook;
		this.oauth_identities = oauth_identities;
		this.oauth_states = oauth_states;
	}

	public static sql_database open() throws SQLException, IOException {
		sql_database_connection connection = sql_database_connection.open();
		Connection jdbc = connection.connection();
		dns_phonebook_repository phonebook_repository = new dns_phonebook_repository(jdbc);
		server_browser_org_phonebook.seed_from_file(phonebook_repository);
		return new sql_database(
				connection,
				new user_repository(jdbc),
				new api_key_repository(jdbc),
				new session_repository(jdbc),
				new audit_log_repository(jdbc),
				new connection_history_repository(jdbc),
				new cache_index_repository(jdbc),
				phonebook_repository,
				new oauth_identity_repository(jdbc),
				new oauth_state_repository(jdbc));
	}

	public user_repository users() {
		return users;
	}

	public api_key_repository api_keys() {
		return api_keys;
	}

	public session_repository sessions() {
		return sessions;
	}

	public audit_log_repository audit_logs() {
		return audit_logs;
	}

	public connection_history_repository connection_history() {
		return connection_history;
	}

	public cache_index_repository cache_indexes() {
		return cache_indexes;
	}

	public dns_phonebook_repository dns_phonebook() {
		return dns_phonebook;
	}

	public oauth_identity_repository oauth_identities() {
		return oauth_identities;
	}

	public oauth_state_repository oauth_states() {
		return oauth_states;
	}

	public Connection connection() {
		return database_connection.connection();
	}

	public void purge_expired() throws SQLException {
		Instant now = Instant.now();
		sessions.delete_expired(now);
		cache_indexes.delete_expired(now);
		oauth_states.delete_expired(now);
	}

	@Override
	public void close() throws SQLException {
		database_connection.close();
	}
}
