/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;

/**
 * Connection manager facade composing lifecycle tracking and pooling.
 */
public final class connection_manager {
	private final connection_lifecycle lifecycle;
	private final connection_pool pool;

	public connection_manager() {
		this(new connection_lifecycle());
	}

	public connection_manager(connection_lifecycle lifecycle) {
		this(lifecycle, new connection_pool(lifecycle));
	}

	public connection_manager(connection_lifecycle lifecycle, connection_pool pool) {
		this.lifecycle = lifecycle;
		this.pool = pool;
	}

	public connection_lifecycle lifecycle() {
		return lifecycle;
	}

	public connection_pool pool() {
		return pool;
	}

	public connection_lifecycle.managed_connection open_connection(
			String host,
			int port,
			int connect_timeout_ms) throws IOException {
		return pool.acquire_connection(host, port, connect_timeout_ms);
	}

	public boolean monitor_health(String connection_id) {
		return lifecycle.monitor_health(connection_id);
	}

	public void release_connection(connection_lifecycle.managed_connection connection) {
		pool.return_connection(connection);
	}

	public void close_connection(String connection_id) {
		lifecycle.close_connection(connection_id);
	}

	public void cleanup_idle_connections() {
		pool.cleanup_idle_connections();
	}
}
