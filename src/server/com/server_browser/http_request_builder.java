/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds outbound HTTP/1.1 request messages.
 */
public final class http_request_builder {
	public record http_request(
			String method,
			URI uri,
			Map<String, String> headers,
			byte[] body) {
	}

	public http_request build_get(URI uri) {
		return build("GET", uri, Map.of(), new byte[0]);
	}

	public http_request build(String method, URI uri, Map<String, String> headers, byte[] body) {
		if (method == null || method.isBlank()) {
			throw new IllegalArgumentException("method is required");
		}
		if (uri == null) {
			throw new IllegalArgumentException("uri is required");
		}
		Map<String, String> normalized = new LinkedHashMap<>();
		if (headers != null) {
			headers.forEach((key, value) -> {
				if (key != null && value != null) {
					normalized.put(key, value);
				}
			});
		}
		byte[] payload = body == null ? new byte[0] : body;
		if (payload.length > 0 && !normalized.containsKey("Content-Length")) {
			normalized.put("Content-Length", String.valueOf(payload.length));
		}
		if (!normalized.containsKey("Host")) {
			int port = uri.getPort();
			String host = uri.getHost();
			if (port > 0 && port != 80 && port != 443) {
				host = host + ":" + port;
			}
			normalized.put("Host", host);
		}
		if (!normalized.containsKey("Connection")) {
			normalized.put("Connection", "close");
		}
		return new http_request(method.toUpperCase(), uri, normalized, payload);
	}

	public byte[] to_wire_bytes(http_request request) {
		String path = request.uri().getRawPath();
		if (path == null || path.isEmpty()) {
			path = "/";
		}
		String query = request.uri().getRawQuery();
		if (query != null && !query.isEmpty()) {
			path = path + "?" + query;
		}
		StringBuilder wire = new StringBuilder();
		wire.append(request.method()).append(' ').append(path).append(" HTTP/1.1\r\n");
		for (Map.Entry<String, String> header : request.headers().entrySet()) {
			wire.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
		}
		wire.append("\r\n");
		byte[] head = wire.toString().getBytes(StandardCharsets.US_ASCII);
		if (request.body().length == 0) {
			return head;
		}
		byte[] message = new byte[head.length + request.body().length];
		System.arraycopy(head, 0, message, 0, head.length);
		System.arraycopy(request.body(), 0, message, head.length, request.body().length);
		return message;
	}

	public String request_line(http_request request) {
		return request.method() + " " + request.uri() + " HTTP/1.1";
	}

	public String headers_as_text(http_request request) {
		return request.headers().entrySet().stream()
				.map(entry -> entry.getKey() + ": " + entry.getValue())
				.collect(Collectors.joining("\n"));
	}
}
