/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Open, track, health-check, and close outbound connections.
 */
public final class connection_lifecycle {
	public enum connection_state {
		NEW,
		OPEN,
		IDLE,
		UNHEALTHY,
		CLOSED
	}

	public record managed_connection(
			String connection_id,
			Socket socket,
			String remote_key,
			connection_state state,
			Instant opened_at,
			Instant last_activity) {
	}

	private final Duration health_timeout;
	private final Map<String, managed_connection> connections = new ConcurrentHashMap<>();

	public connection_lifecycle() {
		this(Duration.ofMinutes(5));
	}

	public connection_lifecycle(Duration health_timeout) {
		this.health_timeout = health_timeout;
	}

	public managed_connection open_connection(String host, int port, int connect_timeout_ms) throws IOException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(host, port), connect_timeout_ms);
		Instant now = Instant.now();
		managed_connection connection = new managed_connection(
				UUID.randomUUID().toString(),
				socket,
				remote_key(host, port),
				connection_state.OPEN,
				now,
				now);
		connections.put(connection.connection_id(), connection);
		return connection;
	}

	public Optional<connection_state> track_state(String connection_id) {
		managed_connection connection = connections.get(connection_id);
		if (connection == null) {
			return Optional.empty();
		}
		connection_state state = evaluate_state(connection);
		connections.put(connection_id, with_state(connection, state));
		return Optional.of(state);
	}

	public boolean monitor_health(String connection_id) {
		Optional<connection_state> state = track_state(connection_id);
		return state.isPresent() && state.get() == connection_state.OPEN;
	}

	public boolean close_connection(String connection_id) {
		managed_connection connection = connections.remove(connection_id);
		if (connection == null) {
			return false;
		}
		close_socket(connection.socket());
		return true;
	}

	public Optional<managed_connection> get_connection(String connection_id) {
		return Optional.ofNullable(connections.get(connection_id));
	}

	public int active_connection_count() {
		return connections.size();
	}

	public void mark_idle(String connection_id) {
		managed_connection connection = connections.get(connection_id);
		if (connection != null) {
			connections.put(connection_id, with_state(connection, connection_state.IDLE));
		}
	}

	public void touch_activity(String connection_id) {
		managed_connection connection = connections.get(connection_id);
		if (connection != null) {
			connections.put(connection_id, new managed_connection(
					connection.connection_id(),
					connection.socket(),
					connection.remote_key(),
					connection.state() == connection_state.NEW ? connection_state.OPEN : connection.state(),
					connection.opened_at(),
					Instant.now()));
		}
	}

	managed_connection with_state(managed_connection connection, connection_state state) {
		return new managed_connection(
				connection.connection_id(),
				connection.socket(),
				connection.remote_key(),
				state,
				connection.opened_at(),
				connection.last_activity());
	}

	private connection_state evaluate_state(managed_connection connection) {
		Socket socket = connection.socket();
		if (socket == null || socket.isClosed() || !socket.isConnected()) {
			return connection_state.CLOSED;
		}
		if (connection.last_activity().plus(health_timeout).isBefore(Instant.now())) {
			return connection_state.UNHEALTHY;
		}
		return connection.state() == connection_state.IDLE ? connection_state.IDLE : connection_state.OPEN;
	}

	static String remote_key(String host, int port) {
		return host.toLowerCase() + ":" + port;
	}

	static void close_socket(Socket socket) {
		if (socket == null) {
			return;
		}
		try {
			socket.close();
		} catch (IOException ignored) {
		}
	}
}
