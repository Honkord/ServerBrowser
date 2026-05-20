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

/**
 * Decodes HTTP/1.1 chunked transfer-encoded bodies.
 */
public final class http_chunked_transfer_handling {
	public byte[] decode(byte[] chunked_body) throws IOException {
		if (chunked_body == null || chunked_body.length == 0) {
			return new byte[0];
		}
		return decode(new java.io.ByteArrayInputStream(chunked_body));
	}

	public byte[] decode(InputStream input) throws IOException {
		ByteArrayOutputStream decoded = new ByteArrayOutputStream();
		while (true) {
			String size_line = read_line(input);
			if (size_line == null) {
				break;
			}
			int chunk_size = Integer.parseInt(size_line.trim().split(";", 2)[0].trim(), 16);
			if (chunk_size == 0) {
				read_line(input);
				break;
			}
			byte[] chunk = input.readNBytes(chunk_size);
			decoded.write(chunk);
			read_line(input);
		}
		return decoded.toByteArray();
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
