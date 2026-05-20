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
import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * Receives a raw HTTP response stream from a remote website.
 */
public final class remote_response_receiver {
	public record remote_response(Socket socket, InputStream raw_stream) implements AutoCloseable {
		@Override
		public void close() throws IOException {
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
		}
	}

	private final http_https_processor http_processor;

	public remote_response_receiver() {
		this(new http_https_processor());
	}

	public remote_response_receiver(http_https_processor http_processor) {
		this.http_processor = http_processor;
	}

	public Optional<remote_response> receive(URI uri, String method, Map<String, String> headers, byte[] body) {
		return http_processor.open_remote_stream(uri, method, headers, body)
				.map(stream -> new remote_response(stream.socket(), stream.response_stream()));
	}
}
