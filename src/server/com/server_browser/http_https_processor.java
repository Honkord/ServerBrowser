/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * HTTP/HTTPS processor facade composing request, header, cookie, compression,
 * redirect, response, chunked, and streaming components.
 */
public final class http_https_processor {
	public record open_remote_stream_result(Socket socket, InputStream response_stream) implements AutoCloseable {
		@Override
		public void close() throws IOException {
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
		}
	}

	private final http_request_builder request_builder;
	private final http_header_processing header_processing;
	private final http_cookie_handling cookie_handling;
	private final http_compression_support compression_support;
	private final http_redirect_processing redirect_processing;
	private final http_response_parser response_parser;
	private final http_chunked_transfer_handling chunked_transfer;
	private final http_streaming_support streaming_support;
	private final int connect_timeout_ms;
	private final int read_timeout_ms;

	public http_https_processor() {
		this(
				new http_request_builder(),
				new http_header_processing(),
				new http_cookie_handling(),
				new http_compression_support(),
				new http_redirect_processing(),
				new http_response_parser(),
				new http_chunked_transfer_handling(),
				new http_streaming_support(),
				10_000,
				20_000);
	}

	public http_https_processor(
			http_request_builder request_builder,
			http_header_processing header_processing,
			http_cookie_handling cookie_handling,
			http_compression_support compression_support,
			http_redirect_processing redirect_processing,
			http_response_parser response_parser,
			http_chunked_transfer_handling chunked_transfer,
			http_streaming_support streaming_support,
			int connect_timeout_ms) {
		this(
				request_builder,
				header_processing,
				cookie_handling,
				compression_support,
				redirect_processing,
				response_parser,
				chunked_transfer,
				streaming_support,
				connect_timeout_ms,
				Math.max(connect_timeout_ms, 20_000));
	}

	public http_https_processor(
			http_request_builder request_builder,
			http_header_processing header_processing,
			http_cookie_handling cookie_handling,
			http_compression_support compression_support,
			http_redirect_processing redirect_processing,
			http_response_parser response_parser,
			http_chunked_transfer_handling chunked_transfer,
			http_streaming_support streaming_support,
			int connect_timeout_ms,
			int read_timeout_ms) {
		this.request_builder = request_builder;
		this.header_processing = header_processing;
		this.cookie_handling = cookie_handling;
		this.compression_support = compression_support;
		this.redirect_processing = redirect_processing;
		this.response_parser = response_parser;
		this.chunked_transfer = chunked_transfer;
		this.streaming_support = streaming_support;
		this.connect_timeout_ms = connect_timeout_ms;
		this.read_timeout_ms = read_timeout_ms;
	}

	public http_request_builder requests() {
		return request_builder;
	}

	public http_header_processing headers() {
		return header_processing;
	}

	public http_cookie_handling cookies() {
		return cookie_handling;
	}

	public http_compression_support compression() {
		return compression_support;
	}

	public http_redirect_processing redirects() {
		return redirect_processing;
	}

	public http_response_parser responses() {
		return response_parser;
	}

	public http_chunked_transfer_handling chunked() {
		return chunked_transfer;
	}

	public http_streaming_support streaming() {
		return streaming_support;
	}

	public Optional<http_response_parser.http_response> fetch(
			URI uri,
			String method,
			Map<String, String> headers,
			byte[] body,
			Map<String, String> cookie_jar) {
		Map<String, String> cookies = cookie_jar == null ? new LinkedHashMap<>() : new LinkedHashMap<>(cookie_jar);
		URI current = uri;
		int redirects_followed = 0;
		try {
			while (true) {
				protocol_selector.protocol protocol = select_protocol(current);
				Map<String, String> request_headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
				cookie_handling.apply_cookie_header(request_headers, cookies);
				http_request_builder.http_request request =
						request_builder.build(method, current, request_headers, body);
				http_response_parser.http_response response = send_and_parse(protocol, current, request);
				cookie_handling.store_cookies(cookies, cookie_handling.parse_set_cookie_headers(
						header_processing.all_values(response.headers(), "Set-Cookie")));

				if (!redirect_processing.is_redirect(response.status_code())) {
					return Optional.of(finalize_response(response));
				}
				if (!redirect_processing.should_follow(redirects_followed)) {
					return Optional.of(finalize_response(response));
				}
				Optional<URI> next_uri = redirect_processing.resolve_redirect(
						response.status_code(),
						response.headers(),
						current);
				if (next_uri.isEmpty()) {
					return Optional.of(finalize_response(response));
				}
				current = next_uri.get();
				redirects_followed++;
				method = "GET";
				body = new byte[0];
			}
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	public long stream_response_body(InputStream source, OutputStream sink) throws IOException {
		return streaming_support.stream_body(source, sink);
	}

	/**
	 * Opens a remote connection and returns the unread raw HTTP response stream.
	 */
	public Optional<open_remote_stream_result> open_remote_stream(
			URI uri,
			String method,
			Map<String, String> headers,
			byte[] body) {
		try {
			protocol_selector.protocol protocol = select_protocol(uri);
			Map<String, String> request_headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
			byte[] payload = body == null ? new byte[0] : body;
			http_request_builder.http_request request =
					request_builder.build(method, uri, request_headers, payload);
			int port = resolve_port(uri, protocol);
			Socket socket = open_socket(protocol, uri.getHost(), port);
			OutputStream output = socket.getOutputStream();
			output.write(request_builder.to_wire_bytes(request));
			output.flush();
			return Optional.of(new open_remote_stream_result(socket, socket.getInputStream()));
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	private http_response_parser.http_response send_and_parse(
			protocol_selector.protocol protocol,
			URI uri,
			http_request_builder.http_request request) throws IOException {
		int port = resolve_port(uri, protocol);
		try (Socket socket = open_socket(protocol, uri.getHost(), port)) {
			OutputStream output = socket.getOutputStream();
			output.write(request_builder.to_wire_bytes(request));
			output.flush();
			return response_parser.parse(socket.getInputStream());
		}
	}

	private http_response_parser.http_response finalize_response(http_response_parser.http_response response)
			throws IOException {
		Optional<String> encoding = header_processing.first_value(response.headers(), "Content-Encoding");
		if (encoding.isEmpty()) {
			return response;
		}
		byte[] decompressed = compression_support.decompress(response.body(), encoding.get());
		return new http_response_parser.http_response(
				response.status_code(),
				response.reason_phrase(),
				response.headers(),
				decompressed,
				response.chunked());
	}

	private Socket open_socket(protocol_selector.protocol protocol, String host, int port) throws IOException {
		if (protocol == protocol_selector.protocol.HTTPS) {
			SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket socket = (SSLSocket) factory.createSocket();
			socket.connect(new java.net.InetSocketAddress(host, port), connect_timeout_ms);
			socket.setSoTimeout(read_timeout_ms);
			socket.startHandshake();
			return socket;
		}
		Socket socket = new Socket();
		socket.connect(new java.net.InetSocketAddress(host, port), connect_timeout_ms);
		socket.setSoTimeout(read_timeout_ms);
		return socket;
	}

	private static protocol_selector.protocol select_protocol(URI uri) {
		if ("https".equalsIgnoreCase(uri.getScheme())) {
			return protocol_selector.protocol.HTTPS;
		}
		if ("http".equalsIgnoreCase(uri.getScheme())) {
			return protocol_selector.protocol.HTTP;
		}
		int port = uri.getPort();
		return port == 443 ? protocol_selector.protocol.HTTPS : protocol_selector.protocol.HTTP;
	}

	private static int resolve_port(URI uri, protocol_selector.protocol protocol) {
		if (uri.getPort() > 0) {
			return uri.getPort();
		}
		return protocol == protocol_selector.protocol.HTTPS ? 443 : 80;
	}
}
