/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reserves and releases local ports during outbound connection setup.
 */
public final class port_reservation {
	private final Set<Integer> reserved_ports = ConcurrentHashMap.newKeySet();

	public boolean reserve(int port) {
		if (port < 1 || port > 65_535) {
			return false;
		}
		return reserved_ports.add(port);
	}

	public void release(int port) {
		reserved_ports.remove(port);
	}

	public boolean is_reserved(int port) {
		return reserved_ports.contains(port);
	}

	public int reserved_count() {
		return reserved_ports.size();
	}
}
