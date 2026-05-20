/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses HTTP/1.1 response status, headers, and body bytes.
 */
public final class http_response_parser {
	public record http_response(
			int status_code,
			String reason_phrase,
			Map<String, List<String>> headers,
			byte[] body,
			boolean chunked) {
	}

	private final http_header_processing header_processing = new http_header_processing();
	private final http_chunked_transfer_handling chunked_transfer = new http_chunked_transfer_handling();

	public http_response parse(byte[] raw_response) throws IOException {
		return parse(new java.io.ByteArrayInputStream(raw_response));
	}

	public http_response parse(InputStream input) throws IOException {
		String status_line = read_line(input);
		if (status_line == null || status_line.isBlank()) {
			throw new IOException("Missing HTTP status line");
		}
		String[] status_parts = status_line.split(" ", 3);
		if (status_parts.length < 2) {
			throw new IOException("Malformed HTTP status line");
		}
		int status_code = Integer.parseInt(status_parts[1]);
		String reason = status_parts.length > 2 ? status_parts[2] : "";

		List<String> header_lines = new ArrayList<>();
		while (true) {
			String line = read_line(input);
			if (line == null || line.isEmpty()) {
				break;
			}
			header_lines.add(line);
		}
		Map<String, List<String>> headers = header_processing.parse_header_block(header_lines);
		boolean chunked = header_processing.first_value(headers, "Transfer-Encoding")
				.map(value -> value.toLowerCase().contains("chunked"))
				.orElse(false);

		byte[] body = read_body(input, headers, chunked);
		return new http_response(status_code, reason, headers, body, chunked);
	}

	private byte[] read_body(InputStream input, Map<String, List<String>> headers, boolean chunked)
			throws IOException {
		if (chunked) {
			ByteArrayOutputStream encoded = new ByteArrayOutputStream();
			input.transferTo(encoded);
			return chunked_transfer.decode(encoded.toByteArray());
		}
		int content_length = header_processing.first_value(headers, "Content-Length")
				.map(Integer::parseInt)
				.orElse(-1);
		if (content_length >= 0) {
			return input.readNBytes(content_length);
		}
		return input.readAllBytes();
	}

	private static String read_line(InputStream input) throws IOException {
		ByteArrayOutputStream line = new ByteArrayOutputStream();
		while (true) {
			int value = input.read();
			if (value < 0) {
				return line.size() == 0 ? null : line.toString(StandardCharsets.US_ASCII);
			}
			if (value == '\n') {
				break;
			}
			if (value != '\r') {
				line.write(value);
			}
		}
		return line.toString(StandardCharsets.US_ASCII);
	}
}
