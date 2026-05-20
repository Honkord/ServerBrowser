/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.net.Socket;

/**
 * Gracefully shuts down TCP client connections.
 */
public final class tcp_connection_shutdown {
	public void shutdown_output(Socket socket) throws IOException {
		if (socket != null && !socket.isClosed()) {
			socket.shutdownOutput();
		}
	}

	public void shutdown_input(Socket socket) throws IOException {
		if (socket != null && !socket.isClosed()) {
			socket.shutdownInput();
		}
	}

	public void close(Socket socket) {
		if (socket == null) {
			return;
		}
		try {
			socket.close();
		} catch (IOException ignored) {
		}
	}
}
