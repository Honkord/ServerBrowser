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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses the HTTP status line and response header block from a live stream.
 */
public final class response_header_block_parser {
	public record parsed_response_header(
			int status_code,
			String reason_phrase,
			Map<String, List<String>> headers,
			byte[] raw_header_block,
			boolean chunked) {
	}

	private final response_header_line_reader line_reader = new response_header_line_reader();

	public parsed_response_header parse(InputStream input) throws IOException {
		String status_line = line_reader.read_line(input);
		if (status_line == null || status_line.isBlank()) {
			throw new IOException("Missing HTTP status line");
		}
		String[] status_parts = status_line.split(" ", 3);
		if (status_parts.length < 2) {
			throw new IOException("Malformed HTTP status line");
		}
		int status_code = Integer.parseInt(status_parts[1]);
		String reason = status_parts.length > 2 ? status_parts[2] : "";

		ByteArrayOutputStream raw = new ByteArrayOutputStream();
		raw.write(status_line.getBytes(StandardCharsets.US_ASCII));
		raw.write("\r\n".getBytes(StandardCharsets.US_ASCII));

		Map<String, List<String>> headers = new LinkedHashMap<>();
		while (true) {
			String line = line_reader.read_line(input);
			if (line == null || line.isEmpty()) {
				raw.write("\r\n".getBytes(StandardCharsets.US_ASCII));
				break;
			}
			raw.write(line.getBytes(StandardCharsets.US_ASCII));
			raw.write("\r\n".getBytes(StandardCharsets.US_ASCII));
			int separator = line.indexOf(':');
			if (separator <= 0) {
				continue;
			}
			String name = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
			String value = line.substring(separator + 1).trim();
			headers.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
		}

		boolean chunked = headers.getOrDefault("transfer-encoding", List.of()).stream()
				.anyMatch(value -> value.toLowerCase(Locale.ROOT).contains("chunked"));

		return new parsed_response_header(
				status_code,
				reason,
				headers,
				raw.toByteArray(),
				chunked);
	}
}
