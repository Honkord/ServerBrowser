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
 * Reads individual lines from a streaming HTTP response.
 */
public final class response_header_line_reader {
	public String read_line(InputStream input) throws IOException {
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
