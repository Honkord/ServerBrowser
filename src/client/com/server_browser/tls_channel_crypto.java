/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;

/**
 * Exposes encrypted TLS socket streams for application data.
 */
public final class tls_channel_crypto {
	public OutputStream encrypting_output(SSLSocket socket) throws IOException {
		if (socket == null) {
			throw new IllegalArgumentException("socket is required");
		}
		return socket.getOutputStream();
	}

	public InputStream decrypting_input(SSLSocket socket) throws IOException {
		if (socket == null) {
			throw new IllegalArgumentException("socket is required");
		}
		return socket.getInputStream();
	}

	public int send_encrypted(SSLSocket socket, byte[] data) throws IOException {
		OutputStream output = encrypting_output(socket);
		output.write(data);
		output.flush();
		return data.length;
	}

	public int receive_decrypted(SSLSocket socket, byte[] buffer) throws IOException {
		return decrypting_input(socket).read(buffer);
	}
}
