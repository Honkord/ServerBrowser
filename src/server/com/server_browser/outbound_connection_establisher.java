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
 * Establishes an outbound TCP connection on a bound local socket.
 */
public final class outbound_connection_establisher {
	public Socket establish(Socket socket, String host, int port, int connect_timeout_ms) throws IOException {
		if (socket == null) {
			throw new IllegalArgumentException("socket is required");
		}
		if (host == null || host.isBlank()) {
			throw new IllegalArgumentException("host is required");
		}
		socket.connect(new InetSocketAddress(host, port), connect_timeout_ms);
		return socket;
	}
}
