/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

/**
 * Legacy server base type. Use {@link server_runtime} to start the full application.
 */
public abstract class server {
	public abstract String retrieve_response();

	public void host_server() throws Exception {
		server_runtime.bootstrap().host_server();
	}

	public static void main(String[] args) throws Exception {
		server_runtime.main(args);
	}
}
