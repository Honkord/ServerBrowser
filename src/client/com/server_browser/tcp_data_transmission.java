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
import java.net.Socket;

/**
 * Sends and receives bytes on an established TCP socket.
 */
public final class tcp_data_transmission {
	public int send(Socket socket, byte[] data) throws IOException {
		if (socket == null || data == null) {
			throw new IllegalArgumentException("socket and data are required");
		}
		OutputStream output = socket.getOutputStream();
		output.write(data);
		output.flush();
		return data.length;
	}

	public int receive(Socket socket, byte[] buffer) throws IOException {
		if (socket == null || buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("socket and buffer are required");
		}
		InputStream input = socket.getInputStream();
		return input.read(buffer);
	}
}
