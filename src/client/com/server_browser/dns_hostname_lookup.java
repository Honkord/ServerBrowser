/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Performs hostname to address resolution.
 */
public final class dns_hostname_lookup {
	public InetAddress[] lookup(String hostname) throws UnknownHostException {
		if (hostname == null || hostname.isBlank()) {
			throw new UnknownHostException("hostname is required");
		}
		return InetAddress.getAllByName(hostname.trim());
	}
}
