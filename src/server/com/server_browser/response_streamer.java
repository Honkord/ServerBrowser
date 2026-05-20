/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * Server-side response streamer facade: receive remote response and stream to client.
 * Response headers are parsed on the client by {@code stream_response_header}.
 */
public final class response_streamer {
	public record stream_result(long bytes_streamed, boolean success) {
	}

	private final remote_response_receiver receiver;
	private final content_stream_to_client content_streamer;
	private final stream_timeout_retry timeout_retry;
	private final sql_database database;
	private final int stream_timeout_ms;

	public response_streamer() {
		this(null, new remote_response_receiver(), new content_stream_to_client(), new stream_timeout_retry(), 15_000);
	}

	public response_streamer(sql_database database) {
		this(database, new remote_response_receiver(), new content_stream_to_client(), new stream_timeout_retry(), 15_000);
	}

	public response_streamer(
			sql_database database,
			remote_response_receiver receiver,
			content_stream_to_client content_streamer,
			stream_timeout_retry timeout_retry,
			int stream_timeout_ms) {
		this.database = database;
		this.receiver = receiver;
		this.content_streamer = content_streamer;
		this.timeout_retry = timeout_retry;
		this.stream_timeout_ms = stream_timeout_ms;
	}

	public remote_response_receiver receiver() {
		return receiver;
	}

	public content_stream_to_client content() {
		return content_streamer;
	}

	public stream_timeout_retry timeouts() {
		return timeout_retry;
	}

	/**
	 * Receives the remote HTTP response and streams the raw bytes (headers + body) to the client.
	 * The client-side {@code stream_response_header} parses the header block from this stream.
	 */
	public stream_result stream_proxy_response(
			OutputStream client_sink,
			URI remote_uri,
			String method,
			Map<String, String> request_headers,
			byte[] request_body,
			String user_id) {
		long history_id = record_connection_start(user_id, remote_uri);
		Optional<remote_response_receiver.remote_response> remote =
				receiver.receive(remote_uri, method, request_headers, request_body);
		if (remote.isEmpty()) {
			return new stream_result(0, false);
		}
		try (remote_response_receiver.remote_response response = remote.get()) {
			timeout_retry.configure_socket_timeout(response.socket(), stream_timeout_ms);
			long bytes = timeout_retry.execute(() ->
					content_streamer.stream_to_client(response.raw_stream(), client_sink, -1));
			record_connection_complete(history_id, bytes);
			return new stream_result(bytes, true);
		} catch (IOException e) {
			return new stream_result(0, false);
		}
	}

	public stream_result stream_proxy_response(
			OutputStream client_sink,
			URI remote_uri,
			String method,
			Map<String, String> request_headers,
			byte[] request_body) {
		return stream_proxy_response(client_sink, remote_uri, method, request_headers, request_body, null);
	}

	private long record_connection_start(String user_id, URI remote_uri) {
		if (database == null || remote_uri.getHost() == null) {
			return -1;
		}
		try {
			String protocol = remote_uri.getScheme() == null ? "https" : remote_uri.getScheme();
			int port = remote_uri.getPort() > 0 ? remote_uri.getPort() : protocol.equalsIgnoreCase("http") ? 80 : 443;
			return database.connection_history().insert_started(user_id, remote_uri.getHost(), port, protocol);
		} catch (SQLException e) {
			text_printer.print(text_printer.format.ERROR, "Failed to record connection: " + e.getMessage());
			return -1;
		}
	}

	private void record_connection_complete(long history_id, long bytes_streamed) {
		if (database == null || history_id < 0) {
			return;
		}
		try {
			database.connection_history().complete(history_id, bytes_streamed);
		} catch (SQLException e) {
			text_printer.print(text_printer.format.ERROR, "Failed to complete connection history: " + e.getMessage());
		}
	}
}
