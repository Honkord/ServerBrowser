/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * TCP client engine facade: socket creation through shutdown.
 */
public final class tcp_client_engine {
	private final tcp_socket_factory socket_factory;
	private final tcp_connection_establishment connection_establishment;
	private final tcp_data_transmission data_transmission;
	private final tcp_connection_shutdown connection_shutdown;

	public tcp_client_engine() {
		this(
				new tcp_socket_factory(),
				new tcp_connection_establishment(),
				new tcp_data_transmission(),
				new tcp_connection_shutdown());
	}

	public tcp_client_engine(
			tcp_socket_factory socket_factory,
			tcp_connection_establishment connection_establishment,
			tcp_data_transmission data_transmission,
			tcp_connection_shutdown connection_shutdown) {
		this.socket_factory = socket_factory;
		this.connection_establishment = connection_establishment;
		this.data_transmission = data_transmission;
		this.connection_shutdown = connection_shutdown;
	}

	public Socket open_connection(InetAddress address, int port, int connect_timeout_ms) throws IOException {
		Socket socket = socket_factory.create_socket();
		return connection_establishment.connect(socket, address, port, connect_timeout_ms);
	}

	public int transmit(Socket socket, byte[] data) throws IOException {
		return data_transmission.send(socket, data);
	}

	public int receive(Socket socket, byte[] buffer) throws IOException {
		return data_transmission.receive(socket, buffer);
	}

	public void close(Socket socket) {
		connection_shutdown.close(socket);
	}
}
