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
 * Reads a proxied raw HTTP response from the server, handles headers on the client,
 * and streams the body to a sink.
 */
public final class proxy_response_reader {
	private final stream_response_header response_header;
	private final int body_buffer_size;

	public proxy_response_reader() {
		this(new stream_response_header(), 8 * 1024);
	}

	public proxy_response_reader(stream_response_header response_header, int body_buffer_size) {
		this.response_header = response_header;
		this.body_buffer_size = body_buffer_size;
	}

	public stream_response_header.header_and_body_stream read_from_server(InputStream server_stream)
			throws IOException {
		return response_header.read_headers(server_stream);
	}

	public long stream_body_to_client(
			stream_response_header.header_and_body_stream response,
			OutputStream body_sink) throws IOException {
		byte[] buffer = new byte[body_buffer_size];
		long total = 0;
		int read;
		InputStream body = response.body_stream();
		while ((read = body.read(buffer)) >= 0) {
			if (read == 0) {
				continue;
			}
			body_sink.write(buffer, 0, read);
			body_sink.flush();
			total += read;
		}
		return total;
	}

	public void forward_headers_to_client(
			OutputStream header_sink,
			response_header_block_parser.parsed_response_header header) throws IOException {
		response_header.forward_headers(header_sink, header);
	}
}
