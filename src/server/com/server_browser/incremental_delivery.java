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
 * Delivers response content to the client in incremental chunks.
 */
public final class incremental_delivery {
	public long deliver(InputStream source, OutputStream sink, int chunk_size) throws IOException {
		if (source == null || sink == null) {
			throw new IllegalArgumentException("source and sink are required");
		}
		if (chunk_size < 1) {
			throw new IllegalArgumentException("chunk_size must be at least 1");
		}
		byte[] buffer = new byte[chunk_size];
		long delivered = 0;
		int read;
		while ((read = source.read(buffer)) >= 0) {
			if (read == 0) {
				continue;
			}
			sink.write(buffer, 0, read);
			sink.flush();
			delivered += read;
		}
		return delivered;
	}
}
