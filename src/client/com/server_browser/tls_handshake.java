/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Performs TLS handshakes over TCP sockets.
 */
public final class tls_handshake {
	private final SSLSocketFactory socket_factory;

	public tls_handshake() {
		this((SSLSocketFactory) SSLSocketFactory.getDefault());
	}

	public tls_handshake(SSLSocketFactory socket_factory) {
		this.socket_factory = socket_factory;
	}

	public SSLSocket perform_handshake(String hostname, int port, int connect_timeout_ms) throws IOException {
		SSLSocket socket = (SSLSocket) socket_factory.createSocket(hostname, port);
		socket.setSoTimeout(connect_timeout_ms);
		configure_endpoint_identification(socket);
		socket.startHandshake();
		return socket;
	}

	public SSLSocket upgrade_socket(javax.net.ssl.SSLSocketFactory factory, java.net.Socket plain, String hostname)
			throws IOException {
		SSLSocket socket = (SSLSocket) factory.createSocket(
				plain,
				hostname,
				plain.getPort(),
				true);
		configure_endpoint_identification(socket);
		socket.startHandshake();
		return socket;
	}

	private static void configure_endpoint_identification(SSLSocket socket) {
		SSLParameters parameters = socket.getSSLParameters();
		parameters.setEndpointIdentificationAlgorithm("HTTPS");
		socket.setSSLParameters(parameters);
	}
}
