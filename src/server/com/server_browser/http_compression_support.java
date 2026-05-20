/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Decompresses gzip and deflate HTTP response bodies.
 */
public final class http_compression_support {
	public byte[] decompress(byte[] body, String content_encoding) throws IOException {
		if (body == null || body.length == 0 || content_encoding == null || content_encoding.isBlank()) {
			return body == null ? new byte[0] : body;
		}
		String encoding = content_encoding.toLowerCase(Locale.ROOT);
		return switch (encoding) {
			case "gzip", "x-gzip" -> gunzip(body);
			case "deflate" -> inflate(body);
			default -> body;
		};
	}

	public boolean is_supported(String content_encoding) {
		if (content_encoding == null || content_encoding.isBlank()) {
			return true;
		}
		String encoding = content_encoding.toLowerCase(Locale.ROOT);
		return encoding.equals("gzip")
				|| encoding.equals("x-gzip")
				|| encoding.equals("deflate")
				|| encoding.equals("identity");
	}

	private static byte[] gunzip(byte[] body) throws IOException {
		try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(body));
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			input.transferTo(output);
			return output.toByteArray();
		}
	}

	private static byte[] inflate(byte[] body) throws IOException {
		try (InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(body));
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			input.transferTo(output);
			return output.toByteArray();
		}
	}
}
