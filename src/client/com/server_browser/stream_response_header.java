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
import java.util.Optional;

/**
 * Client-side response header streaming facade.
 */
public final class stream_response_header {
	public record header_and_body_stream(
			response_header_block_parser.parsed_response_header header,
			InputStream body_stream) {
	}

	private final response_header_block_parser parser;
	private final response_header_block_forwarder forwarder;

	public stream_response_header() {
		this(new response_header_block_parser(), new response_header_block_forwarder());
	}

	public stream_response_header(
			response_header_block_parser parser,
			response_header_block_forwarder forwarder) {
		this.parser = parser;
		this.forwarder = forwarder;
	}

	public response_header_block_parser parser() {
		return parser;
	}

	public response_header_block_forwarder forwarder() {
		return forwarder;
	}

	public header_and_body_stream read_headers(InputStream source) throws IOException {
		response_header_block_parser.parsed_response_header header = parser.parse(source);
		return new header_and_body_stream(header, source);
	}

	public void forward_headers(OutputStream sink, response_header_block_parser.parsed_response_header header)
			throws IOException {
		forwarder.forward_parsed(sink, header);
	}

	public Optional<String> first_header_value(
			response_header_block_parser.parsed_response_header header,
			String name) {
		if (header == null || name == null) {
			return Optional.empty();
		}
		var values = header.headers().get(name.trim().toLowerCase());
		if (values == null || values.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(values.get(0));
	}
}
