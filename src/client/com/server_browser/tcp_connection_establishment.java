/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Establishes TCP connections to resolved remote hosts.
 */
public final class tcp_connection_establishment {
	public Socket connect(Socket socket, InetAddress address, int port, int connect_timeout_ms) throws IOException {
		if (socket == null) {
			throw new IllegalArgumentException("socket is required");
		}
		if (address == null) {
			throw new IllegalArgumentException("address is required");
		}
		socket.connect(new InetSocketAddress(address, port), connect_timeout_ms);
		return socket;
	}
}
