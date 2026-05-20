/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Applies read timeouts and retries for streaming remote responses.
 */
public final class stream_timeout_retry {
	@FunctionalInterface
	public interface stream_operation<T> {
		T execute() throws IOException;
	}

	private final int max_attempts;
	private final long backoff_ms;

	public stream_timeout_retry() {
		this(3, 150);
	}

	public stream_timeout_retry(int max_attempts, long backoff_ms) {
		if (max_attempts < 1) {
			throw new IllegalArgumentException("max_attempts must be at least 1");
		}
		this.max_attempts = max_attempts;
		this.backoff_ms = backoff_ms;
	}

	public <T> T execute(stream_operation<T> operation) throws IOException {
		IOException last_failure = null;
		for (int attempt = 1; attempt <= max_attempts; attempt++) {
			try {
				return operation.execute();
			} catch (IOException e) {
				last_failure = e;
				if (attempt < max_attempts) {
					try {
						Thread.sleep(backoff_ms * attempt);
					} catch (InterruptedException interrupted) {
						Thread.currentThread().interrupt();
						throw new IOException("Streaming interrupted", interrupted);
					}
				}
			}
		}
		throw last_failure;
	}

	public void configure_socket_timeout(Socket socket, int timeout_ms) throws IOException {
		if (socket != null) {
			socket.setSoTimeout(timeout_ms);
		}
	}

	public InputStream with_timeout(InputStream source, Socket socket, int timeout_ms) throws IOException {
		configure_socket_timeout(socket, timeout_ms);
		return source;
	}
}
