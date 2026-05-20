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

/**
 * Streams HTTP response bodies incrementally to a client sink.
 */
public final class http_streaming_support {
	private final int buffer_size;

	public http_streaming_support() {
		this(8 * 1024);
	}

	public http_streaming_support(int buffer_size) {
		if (buffer_size < 1) {
			throw new IllegalArgumentException("buffer_size must be at least 1");
		}
		this.buffer_size = buffer_size;
	}

	public long stream_body(InputStream source, OutputStream sink) throws IOException {
		if (source == null || sink == null) {
			throw new IllegalArgumentException("source and sink are required");
		}
		byte[] buffer = new byte[buffer_size];
		long total = 0;
		int read;
		while ((read = source.read(buffer)) >= 0) {
			if (read == 0) {
				continue;
			}
			sink.write(buffer, 0, read);
			total += read;
		}
		sink.flush();
		return total;
	}

	public long stream_chunked(InputStream source, OutputStream sink, http_chunked_transfer_handling chunked)
			throws IOException {
		byte[] decoded = chunked.decode(source);
		sink.write(decoded);
		sink.flush();
		return decoded.length;
	}
}
