/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reuses connections, limits concurrency, and removes idle sockets.
 */
public final class connection_pool {
	private final connection_lifecycle lifecycle;
	private final int max_concurrent_connections;
	private final Duration idle_timeout;
	private final Map<String, Deque<connection_lifecycle.managed_connection>> idle_by_remote =
			new ConcurrentHashMap<>();

	public connection_pool(connection_lifecycle lifecycle) {
		this(lifecycle, 64, Duration.ofSeconds(60));
	}

	public connection_pool(
			connection_lifecycle lifecycle,
			int max_concurrent_connections,
			Duration idle_timeout) {
		if (max_concurrent_connections < 1) {
			throw new IllegalArgumentException("max_concurrent_connections must be at least 1");
		}
		this.lifecycle = lifecycle;
		this.max_concurrent_connections = max_concurrent_connections;
		this.idle_timeout = idle_timeout;
	}

	public Optional<connection_lifecycle.managed_connection> reuse_existing_connection(String remote_key) {
		Deque<connection_lifecycle.managed_connection> idle = idle_by_remote.get(remote_key);
		if (idle == null) {
			return Optional.empty();
		}
		synchronized (idle) {
			while (!idle.isEmpty()) {
				connection_lifecycle.managed_connection candidate = idle.pollFirst();
				if (lifecycle.monitor_health(candidate.connection_id())) {
					lifecycle.touch_activity(candidate.connection_id());
					return lifecycle.get_connection(candidate.connection_id());
				}
				connection_lifecycle.close_socket(candidate.socket());
			}
		}
		return Optional.empty();
	}

	public connection_lifecycle.managed_connection acquire_connection(
			String host,
			int port,
			int connect_timeout_ms) throws IOException {
		if (at_concurrency_limit()) {
			cleanup_idle_connections();
			if (at_concurrency_limit()) {
				throw new IOException("Concurrent connection limit reached");
			}
		}
		String remote_key = connection_lifecycle.remote_key(host, port);
		Optional<connection_lifecycle.managed_connection> reused = reuse_existing_connection(remote_key);
		if (reused.isPresent()) {
			return reused.get();
		}
		return lifecycle.open_connection(host, port, connect_timeout_ms);
	}

	public void return_connection(connection_lifecycle.managed_connection connection) {
		if (connection == null) {
			return;
		}
		if (!lifecycle.monitor_health(connection.connection_id())) {
			lifecycle.close_connection(connection.connection_id());
			return;
		}
		lifecycle.mark_idle(connection.connection_id());
		lifecycle.touch_activity(connection.connection_id());
		lifecycle.get_connection(connection.connection_id()).ifPresent(active ->
				idle_by_remote
						.computeIfAbsent(connection.remote_key(), key -> new ArrayDeque<>())
						.addLast(active));
	}

	public void cleanup_idle_connections() {
		Instant cutoff = Instant.now().minus(idle_timeout);
		for (Deque<connection_lifecycle.managed_connection> idle : idle_by_remote.values()) {
			synchronized (idle) {
				idle.removeIf(connection -> {
					boolean expired = connection.last_activity().isBefore(cutoff);
					if (expired) {
						lifecycle.close_connection(connection.connection_id());
					}
					return expired;
				});
			}
		}
	}

	public boolean at_concurrency_limit() {
		return lifecycle.active_connection_count() >= max_concurrent_connections;
	}

	public int max_concurrent_connections() {
		return max_concurrent_connections;
	}
}
