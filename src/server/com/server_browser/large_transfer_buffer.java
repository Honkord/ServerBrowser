/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Buffers large remote response transfers before incremental delivery.
 */
public final class large_transfer_buffer {
	private final int buffer_size;
	private final long large_transfer_threshold;

	public large_transfer_buffer() {
		this(64 * 1024, 256 * 1024);
	}

	public large_transfer_buffer(int buffer_size, long large_transfer_threshold) {
		if (buffer_size < 1) {
			throw new IllegalArgumentException("buffer_size must be at least 1");
		}
		this.buffer_size = buffer_size;
		this.large_transfer_threshold = large_transfer_threshold;
	}

	public InputStream buffer_if_large(InputStream source, long expected_size) {
		BufferedInputStream buffered = new BufferedInputStream(source, buffer_size);
		if (expected_size >= large_transfer_threshold) {
			return buffered;
		}
		return buffered;
	}

	public int buffer_size() {
		return buffer_size;
	}

	public long large_transfer_threshold() {
		return large_transfer_threshold;
	}
}
