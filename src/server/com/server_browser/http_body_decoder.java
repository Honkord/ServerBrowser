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
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Decodes {@code Content-Encoding} on outbound HTTP responses.
 * Java's {@link java.net.http.HttpClient} does not decompress {@code ofByteArray()} bodies automatically.
 */
public final class http_body_decoder {
	private http_body_decoder() {
	}

	public static byte[] decode(HttpResponse<byte[]> response) {
		byte[] body = response.body() == null ? new byte[0] : response.body();
		String encoding = response.headers().firstValue("Content-Encoding").orElse("").toLowerCase(Locale.ROOT);
		if (encoding.contains("gzip")) {
			return gunzip(body);
		}
		if (encoding.contains("deflate")) {
			return inflate(body);
		}
		if (is_gzip(body)) {
			return gunzip(body);
		}
		return body;
	}

	private static boolean is_gzip(byte[] body) {
		return body.length >= 2 && body[0] == (byte) 0x1f && body[1] == (byte) 0x8b;
	}

	private static byte[] gunzip(byte[] body) {
		try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(body));
				ByteArrayOutputStream output = new ByteArrayOutputStream(body.length * 2)) {
			input.transferTo(output);
			return output.toByteArray();
		} catch (IOException error) {
			return body;
		}
	}

	private static byte[] inflate(byte[] body) {
		try {
			return inflate_with_nowrap(body, false);
		} catch (IOException zlib_error) {
			try {
				return inflate_with_nowrap(body, true);
			} catch (IOException raw_error) {
				return body;
			}
		}
	}

	private static byte[] inflate_with_nowrap(byte[] body, boolean nowrap) throws IOException {
		Inflater inflater = new Inflater(nowrap);
		try (InputStream input = new InflaterInputStream(new ByteArrayInputStream(body), inflater);
				ByteArrayOutputStream output = new ByteArrayOutputStream(body.length * 2)) {
			input.transferTo(output);
			return output.toByteArray();
		} finally {
			inflater.end();
		}
	}
}
