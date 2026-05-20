/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Binds a client socket to a reserved local port.
 */
public final class socket_binder {
	public Socket bind(int local_port) throws IOException {
		if (local_port < 0) {
			throw new IllegalArgumentException("local_port is required");
		}
		Socket socket = new Socket();
		socket.bind(new InetSocketAddress(local_port));
		return socket;
	}

	public Socket bind_ephemeral() throws IOException {
		Socket socket = new Socket();
		socket.bind(new InetSocketAddress(0));
		return socket;
	}

	public int bound_port(Socket socket) {
		if (socket == null || !socket.isBound()) {
			return -1;
		}
		return socket.getLocalPort();
	}
}
