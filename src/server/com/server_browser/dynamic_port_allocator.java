/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

/**
 * Dynamic port allocator facade: scan, reserve, bind, connect, release.
 */
public final class dynamic_port_allocator {
	public record allocated_connection(Socket socket, int local_port) {
	}

	private final ephemeral_port_scanner port_scanner;
	private final port_reservation port_reservation;
	private final socket_binder socket_binder;
	private final outbound_connection_establisher connection_establisher;
	private final int connect_timeout_ms;

	public dynamic_port_allocator() {
		this(
				new ephemeral_port_scanner(),
				new port_reservation(),
				new socket_binder(),
				new outbound_connection_establisher(),
				10_000);
	}

	public dynamic_port_allocator(
			ephemeral_port_scanner port_scanner,
			port_reservation port_reservation,
			socket_binder socket_binder,
			outbound_connection_establisher connection_establisher,
			int connect_timeout_ms) {
		this.port_scanner = port_scanner;
		this.port_reservation = port_reservation;
		this.socket_binder = socket_binder;
		this.connection_establisher = connection_establisher;
		this.connect_timeout_ms = connect_timeout_ms;
	}

	public ephemeral_port_scanner scanner() {
		return port_scanner;
	}

	public port_reservation reservations() {
		return port_reservation;
	}

	public socket_binder binder() {
		return socket_binder;
	}

	public outbound_connection_establisher establisher() {
		return connection_establisher;
	}

	/**
	 * Runs the full port selection pipeline for an outbound connection.
	 */
	public Optional<allocated_connection> request_connection(String host, int port) {
		Optional<allocated_connection> scanned = request_with_scanned_port(host, port);
		if (scanned.isPresent()) {
			return scanned;
		}
		return request_with_ephemeral_bind(host, port);
	}

	private Optional<allocated_connection> request_with_scanned_port(String host, int remote_port) {
		int local_port = port_scanner.find_free_port();
		if (local_port < 0 || !port_reservation.reserve(local_port)) {
			return Optional.empty();
		}
		return finish_connection(host, remote_port, local_port, () -> socket_binder.bind(local_port));
	}

	private Optional<allocated_connection> request_with_ephemeral_bind(String host, int remote_port) {
		Socket bound = null;
		try {
			bound = socket_binder.bind_ephemeral();
			final Socket bound_socket = bound;
			int local_port = socket_binder.bound_port(bound_socket);
			if (local_port < 0 || !port_reservation.reserve(local_port)) {
				connection_lifecycle.close_socket(bound_socket);
				return Optional.empty();
			}
			return finish_connection(host, remote_port, local_port, () -> bound_socket);
		} catch (IOException e) {
			connection_lifecycle.close_socket(bound);
			return Optional.empty();
		}
	}

	private Optional<allocated_connection> finish_connection(
			String host,
			int remote_port,
			int local_port,
			SocketSupplier socket_supplier) {
		Socket socket = null;
		try {
			socket = socket_supplier.open();
			int bound_port = socket_binder.bound_port(socket);
			connection_establisher.establish(socket, host, remote_port, connect_timeout_ms);
			return Optional.of(new allocated_connection(socket, bound_port > 0 ? bound_port : local_port));
		} catch (IOException e) {
			connection_lifecycle.close_socket(socket);
			port_reservation.release(local_port);
			return Optional.empty();
		}
	}

	@FunctionalInterface
	private interface SocketSupplier {
		Socket open() throws IOException;
	}

	public void release_after_completion(allocated_connection connection) {
		if (connection == null) {
			return;
		}
		connection_lifecycle.close_socket(connection.socket());
		port_reservation.release(connection.local_port());
	}
}
