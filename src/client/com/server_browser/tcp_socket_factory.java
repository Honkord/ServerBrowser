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
 * Creates plain TCP client sockets.
 */
public final class tcp_socket_factory {
	public Socket create_socket() {
		return new Socket();
	}

	public Socket create_bound_socket(int local_port) throws IOException {
		Socket socket = create_socket();
		if (local_port > 0) {
			socket.bind(new InetSocketAddress(local_port));
		} else {
			socket.bind(new InetSocketAddress(0));
		}
		return socket;
	}
}
