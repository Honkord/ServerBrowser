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
 * Streams remote response content to the connected client.
 */
public final class content_stream_to_client {
	private final incremental_delivery incremental_delivery;
	private final large_transfer_buffer transfer_buffer;

	public content_stream_to_client() {
		this(new incremental_delivery(), new large_transfer_buffer());
	}

	public content_stream_to_client(
			incremental_delivery incremental_delivery,
			large_transfer_buffer transfer_buffer) {
		this.incremental_delivery = incremental_delivery;
		this.transfer_buffer = transfer_buffer;
	}

	public long stream_to_client(InputStream remote_source, OutputStream client_sink, long expected_size)
			throws IOException {
		InputStream source = transfer_buffer.buffer_if_large(remote_source, expected_size);
		return incremental_delivery.deliver(source, client_sink, transfer_buffer.buffer_size());
	}
}
