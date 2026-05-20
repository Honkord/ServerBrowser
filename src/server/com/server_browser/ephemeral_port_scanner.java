/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans the ephemeral port range for bindable local ports.
 */
public final class ephemeral_port_scanner {
	public static final int DEFAULT_MIN_PORT = 49_152;
	public static final int DEFAULT_MAX_PORT = 65_535;

	private final int min_port;
	private final int max_port;

	public ephemeral_port_scanner() {
		this(DEFAULT_MIN_PORT, DEFAULT_MAX_PORT);
	}

	public ephemeral_port_scanner(int min_port, int max_port) {
		if (min_port < 1 || max_port < min_port) {
			throw new IllegalArgumentException("invalid ephemeral port range");
		}
		this.min_port = min_port;
		this.max_port = max_port;
	}

	public List<Integer> scan_available_ports(int max_results) {
		List<Integer> available = new ArrayList<>();
		if (max_results < 1) {
			return available;
		}
		for (int port = min_port; port <= max_port && available.size() < max_results; port++) {
			if (is_port_bindable(port)) {
				available.add(port);
			}
		}
		return available;
	}

	public int find_free_port() {
		for (int port = min_port; port <= max_port; port++) {
			if (is_port_bindable(port)) {
				return port;
			}
		}
		return -1;
	}

	public int find_free_port_os_assigned() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			return -1;
		}
	}

	private boolean is_port_bindable(int port) {
		try (ServerSocket socket = new ServerSocket(port)) {
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
