/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session creation, validation, expiration, and logout.
 */
public final class session_manager {
	public record session_record(
			String session_id,
			String user_id,
			String access_token,
			String refresh_token,
			Instant expires_at) {
		public boolean is_expired(Instant now) {
			return !expires_at.isAfter(now);
		}
	}

	private final Duration session_lifetime;
	private final session_repository session_repository;
	private final Map<String, session_record> sessions_by_id = new ConcurrentHashMap<>();
	private final Map<String, String> session_id_by_access_token = new ConcurrentHashMap<>();

	public session_manager(Duration session_lifetime) {
		this(session_lifetime, null);
	}

	public session_manager() {
		this(Duration.ofHours(24));
	}

	public session_manager(Duration session_lifetime, session_repository session_repository) {
		this.session_lifetime = session_lifetime;
		this.session_repository = session_repository;
	}

	public session_record create_session(String user_id, String access_token, String refresh_token) {
		Instant expires_at = Instant.now().plus(session_lifetime);
		session_record session = new session_record(
				UUID.randomUUID().toString(),
				user_id,
				access_token,
				refresh_token,
				expires_at);
		sessions_by_id.put(session.session_id(), session);
		session_id_by_access_token.put(access_token, session.session_id());
		persist_session(session);
		return session;
	}

	public Optional<session_record> validate_session(String access_token) {
		if (access_token == null || access_token.isBlank()) {
			return Optional.empty();
		}
		String session_id = session_id_by_access_token.get(access_token);
		if (session_id == null) {
			Optional<session_record> loaded = load_session(access_token);
			if (loaded.isEmpty()) {
				return Optional.empty();
			}
			session_record session = loaded.get();
			cache_session(session);
			session_id = session.session_id();
		}
		session_record session = sessions_by_id.get(session_id);
		if (session == null || is_expired(session)) {
			remove_session(session_id);
			return Optional.empty();
		}
		return Optional.of(session);
	}

	public boolean is_expired(session_record session) {
		return session == null || session.is_expired(Instant.now());
	}

	public void purge_expired_sessions() {
		Instant now = Instant.now();
		sessions_by_id.values().removeIf(session -> {
			if (session.is_expired(now)) {
				session_id_by_access_token.remove(session.access_token());
				return true;
			}
			return false;
		});
		if (session_repository != null) {
			try {
				session_repository.delete_expired(now);
			} catch (SQLException e) {
				text_printer.print(text_printer.format.ERROR, "Failed to purge sessions: " + e.getMessage());
			}
		}
	}

	public boolean logout(String session_id) {
		session_record removed = sessions_by_id.remove(session_id);
		if (removed == null) {
			return false;
		}
		session_id_by_access_token.remove(removed.access_token());
		delete_persisted_session(session_id);
		return true;
	}

	public boolean logout_by_access_token(String access_token) {
		String session_id = session_id_by_access_token.get(access_token);
		if (session_id == null) {
			Optional<session_record> loaded = load_session(access_token);
			if (loaded.isEmpty()) {
				return false;
			}
			session_id = loaded.get().session_id();
		}
		return logout(session_id);
	}

	private void cache_session(session_record session) {
		sessions_by_id.put(session.session_id(), session);
		session_id_by_access_token.put(session.access_token(), session.session_id());
	}

	private Optional<session_record> load_session(String access_token) {
		if (session_repository == null) {
			return Optional.empty();
		}
		try {
			return session_repository.find_by_access_token(access_token);
		} catch (SQLException e) {
			text_printer.print(text_printer.format.ERROR, "Failed to load session: " + e.getMessage());
			return Optional.empty();
		}
	}

	private void persist_session(session_record session) {
		if (session_repository == null) {
			return;
		}
		try {
			session_repository.insert(session);
		} catch (SQLException e) {
			text_printer.print(text_printer.format.ERROR, "Failed to persist session: " + e.getMessage());
		}
	}

	private void delete_persisted_session(String session_id) {
		if (session_repository == null) {
			return;
		}
		try {
			session_repository.delete_by_session_id(session_id);
		} catch (SQLException e) {
			text_printer.print(text_printer.format.ERROR, "Failed to delete session: " + e.getMessage());
		}
	}

	private void remove_session(String session_id) {
		session_record removed = sessions_by_id.remove(session_id);
		if (removed != null) {
			session_id_by_access_token.remove(removed.access_token());
			delete_persisted_session(session_id);
		}
	}
}
