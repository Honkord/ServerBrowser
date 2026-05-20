/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Forwards a parsed or rebuilt response header block to a client sink.
 */
public final class response_header_block_forwarder {
	public void forward_raw_block(OutputStream sink, byte[] raw_header_block) throws IOException {
		if (sink == null || raw_header_block == null) {
			throw new IllegalArgumentException("sink and raw_header_block are required");
		}
		sink.write(raw_header_block);
		sink.flush();
	}

	public void forward_parsed(OutputStream sink, response_header_block_parser.parsed_response_header header)
			throws IOException {
		if (header == null) {
			throw new IllegalArgumentException("header is required");
		}
		if (header.raw_header_block() != null && header.raw_header_block().length > 0) {
			forward_raw_block(sink, header.raw_header_block());
			return;
		}
		write_line(sink, "HTTP/1.1 " + header.status_code() + " " + header.reason_phrase());
		for (Map.Entry<String, List<String>> entry : header.headers().entrySet()) {
			for (String value : entry.getValue()) {
				write_line(sink, entry.getKey() + ": " + value);
			}
		}
		sink.write("\r\n".getBytes(StandardCharsets.US_ASCII));
		sink.flush();
	}

	private static void write_line(OutputStream sink, String line) throws IOException {
		sink.write(line.getBytes(StandardCharsets.US_ASCII));
		sink.write("\r\n".getBytes(StandardCharsets.US_ASCII));
	}
}
