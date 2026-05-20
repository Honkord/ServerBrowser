/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/**
 * Authentication subsystem facade composing credential, password, session, and access components.
 */
public final class authentication_subsystem {
	public static final String OAUTH_PASSWORD_MARKER = "{oauth-account}";

	private final credential_generator credential_generator;
	private final password_security password_security;
	private final session_manager session_manager;
	private final access_control access;
	private final sql_database database;

	public authentication_subsystem() {
		this(null, new credential_generator(), new password_security(), new session_manager(), new access_control());
	}

	public authentication_subsystem(sql_database database) {
		this(
				database,
				new credential_generator(),
				new password_security(),
				new session_manager(
						java.time.Duration.ofHours(24),
						database == null ? null : database.sessions()),
				new access_control());
	}

	public authentication_subsystem(
			sql_database database,
			credential_generator credential_generator,
			password_security password_security,
			session_manager session_manager,
			access_control access) {
		this.database = database;
		this.credential_generator = credential_generator;
		this.password_security = password_security;
		this.session_manager = session_manager;
		this.access = access;
	}

	public credential_generator credentials() {
		return credential_generator;
	}

	public password_security passwords() {
		return password_security;
	}

	public session_manager sessions() {
		return session_manager;
	}

	public access_control access() {
		return access;
	}

	public Optional<sql_database> database() {
		return Optional.ofNullable(database);
	}

	/**
	 * Registers a user identity with hashed credentials and default role.
	 */
	public registered_user register_user(String username, char[] plain_password) {
		if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("Username is required");
		}
		if (find_user(username).isPresent()) {
			throw new IllegalArgumentException("Username already exists");
		}
		String user_id = credential_generator.create_user_id();
		byte[] salt = password_security.generate_salt();
		String password_hash = password_security.hash_password(plain_password, salt);
		persist_user(user_id, username, password_hash, salt, null, username);
		access.assign_role(user_id, access_control.role.USER);
		audit("user_registered", user_id, "Registered user " + username);
		return new registered_user(user_id, username, password_hash, salt);
	}

	/**
	 * Authenticates credentials and opens a new session.
	 */
	public Optional<session_manager.session_record> login(String username, char[] plain_password) {
		return authenticate(username, plain_password).session();
	}

	public login_result authenticate(String username, char[] plain_password) {
		Optional<registered_user> user = find_user(username);
		if (user.isEmpty()) {
			return new login_result(Optional.empty(), login_outcome.NOT_FOUND);
		}
		if (OAUTH_PASSWORD_MARKER.equals(user.get().password_hash())) {
			return new login_result(Optional.empty(), login_outcome.OAUTH_ACCOUNT);
		}
		if (!password_security.verify_password(plain_password, user.get().password_hash(), user.get().salt())) {
			audit("login_failed", user.get().user_id(), "Invalid password for " + username);
			return new login_result(Optional.empty(), login_outcome.WRONG_PASSWORD);
		}
		access.assign_role(user.get().user_id(), access_control.role.USER);
		String access_token = credential_generator.generate_access_token();
		String refresh_token = credential_generator.generate_refresh_token();
		session_manager.session_record session =
				session_manager.create_session(user.get().user_id(), access_token, refresh_token);
		audit("login_success", user.get().user_id(), "User logged in: " + username);
		return new login_result(Optional.of(session), login_outcome.SUCCESS);
	}

	/** Restores in-memory roles after restart for users already stored in the database. */
	public void restore_roles_from_database() {
		if (database == null) {
			return;
		}
		try {
			for (String user_id : database.users().list_user_ids()) {
				access.assign_role(user_id, access_control.role.USER);
			}
		} catch (SQLException e) {
			text_printer.print(text_printer.format.ERROR, "Failed to restore user roles: " + e.getMessage());
		}
	}

	public Optional<session_manager.session_record> login(
			registered_user user,
			char[] plain_password) {
		if (!password_security.verify_password(plain_password, user.password_hash(), user.salt())) {
			return Optional.empty();
		}
		String access_token = credential_generator.generate_access_token();
		String refresh_token = credential_generator.generate_refresh_token();
		return Optional.of(session_manager.create_session(user.user_id(), access_token, refresh_token));
	}

	public session_manager.session_record login_with_oauth(oauth_provider provider, oauth_client.oauth_user profile)
			throws SQLException {
		if (database == null) {
			throw new IllegalStateException("Database is not available");
		}
		Optional<oauth_identity_repository.oauth_identity_row> existing =
				database.oauth_identities().find(provider.id(), profile.subject());
		if (existing.isPresent()) {
			String user_id = existing.get().user_id();
			access.assign_role(user_id, access_control.role.USER);
			return open_session(user_id, profile.email());
		}
		String username = derive_username(provider, profile);
		if (find_user(username).isPresent()) {
			username = username + "_" + profile.subject().substring(0, Math.min(6, profile.subject().length()));
		}
		String user_id = credential_generator.create_user_id();
		persist_user(
				user_id,
				username,
				OAUTH_PASSWORD_MARKER,
				new byte[0],
				profile.email(),
				profile.display_name());
		database.oauth_identities().insert(new oauth_identity_repository.oauth_identity_row(
				provider.id(),
				profile.subject(),
				user_id,
				profile.email(),
				Instant.now()));
		access.assign_role(user_id, access_control.role.USER);
		audit("oauth_login", user_id, "Signed in with " + provider.id());
		return open_session(user_id, profile.email());
	}

	private session_manager.session_record open_session(String user_id, String email) throws SQLException {
		String access_token = credential_generator.generate_access_token();
		String refresh_token = credential_generator.generate_refresh_token();
		return session_manager.create_session(user_id, access_token, refresh_token);
	}

	private static String derive_username(oauth_provider provider, oauth_client.oauth_user profile) {
		if (profile.email() != null && profile.email().contains("@")) {
			return profile.email().substring(0, profile.email().indexOf('@')).replaceAll("[^a-zA-Z0-9._-]", "");
		}
		if (profile.display_name() != null && !profile.display_name().isBlank()) {
			return profile.display_name().replaceAll("\\s+", ".").replaceAll("[^a-zA-Z0-9._-]", "");
		}
		return provider.id() + "_" + profile.subject().substring(0, Math.min(8, profile.subject().length()));
	}

	public boolean authorize_proxy_request(String access_token) {
		Optional<session_manager.session_record> session = session_manager.validate_session(access_token);
		if (session.isEmpty()) {
			return false;
		}
		return access.authorize_request(
				session.get().user_id(),
				access_control.permission.PROXY_REQUEST);
	}

	public Optional<String> create_api_key(String user_id, String raw_api_key, Instant expires_at) {
		if (database == null || user_id == null || raw_api_key == null) {
			return Optional.empty();
		}
		try {
			String key_id = credential_generator.create_user_id();
			byte[] salt = password_security.generate_salt();
			String hash = password_security.hash_password(raw_api_key.toCharArray(), salt);
			database.api_keys().insert(new api_key_repository.api_key_row(key_id, user_id, hash, expires_at));
			audit("api_key_created", user_id, "API key created");
			return Optional.of(key_id);
		} catch (SQLException e) {
			text_printer.print(text_printer.format.ERROR, "Failed to create API key: " + e.getMessage());
			return Optional.empty();
		}
	}

	private Optional<registered_user> find_user(String username) {
		if (database == null || username == null || username.isBlank()) {
			return Optional.empty();
		}
		try {
			return database.users().find_by_username(username).map(row -> new registered_user(
					row.user_id(),
					row.username(),
					row.password_hash(),
					row.salt()));
		} catch (SQLException e) {
			text_printer.print(text_printer.format.ERROR, "Failed to load user: " + e.getMessage());
			return Optional.empty();
		}
	}

	private void persist_user(
			String user_id,
			String username,
			String password_hash,
			byte[] salt,
			String email,
			String display_name) {
		if (database == null) {
			return;
		}
		try {
			database.users().insert(new user_repository.user_row(
					user_id,
					username,
					password_hash,
					salt,
					email,
					display_name,
					Instant.now(),
					"active"));
		} catch (SQLException e) {
			if (is_unique_username_violation(e)) {
				throw new IllegalArgumentException("Username already exists");
			}
			throw new IllegalStateException("Failed to persist user", e);
		}
	}

	private static boolean is_unique_username_violation(SQLException error) {
		for (SQLException current = error; current != null; current = current.getNextException()) {
			String message = current.getMessage();
			if (message != null && message.contains("users.username")) {
				return true;
			}
		}
		return false;
	}

	private void audit(String event_type, String user_id, String message) {
		if (database == null) {
			return;
		}
		try {
			database.audit_logs().insert(user_id, event_type, message);
		} catch (SQLException e) {
			text_printer.print(text_printer.format.ERROR, "Failed to write audit log: " + e.getMessage());
		}
	}

	public enum login_outcome {
		SUCCESS,
		NOT_FOUND,
		OAUTH_ACCOUNT,
		WRONG_PASSWORD
	}

	public record login_result(Optional<session_manager.session_record> session, login_outcome outcome) {
	}

	public record registered_user(String user_id, String username, String password_hash, byte[] salt) {
	}
}
