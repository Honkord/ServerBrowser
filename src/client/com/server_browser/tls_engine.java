/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.util.Optional;
import javax.net.ssl.SSLSocket;

/**
 * TLS engine facade: handshake, validation, crypto, and channel management.
 */
public final class tls_engine {
	private final tls_handshake handshake;
	private final tls_certificate_validation certificate_validation;
	private final tls_channel_crypto channel_crypto;
	private final tls_secure_channel_manager channel_manager;

	public tls_engine() {
		this(
				new tls_handshake(),
				new tls_certificate_validation(),
				new tls_channel_crypto(),
				new tls_secure_channel_manager());
	}

	public tls_engine(
			tls_handshake handshake,
			tls_certificate_validation certificate_validation,
			tls_channel_crypto channel_crypto,
			tls_secure_channel_manager channel_manager) {
		this.handshake = handshake;
		this.certificate_validation = certificate_validation;
		this.channel_crypto = channel_crypto;
		this.channel_manager = channel_manager;
	}

	public Optional<tls_secure_channel_manager.secure_channel> open_secure_channel(
			String hostname,
			int port,
			int connect_timeout_ms) {
		try {
			SSLSocket socket = handshake.perform_handshake(hostname, port, connect_timeout_ms);
			if (!certificate_validation.validate(socket, hostname)) {
				socket.close();
				return Optional.empty();
			}
			return Optional.of(channel_manager.register(socket, hostname));
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	public int send(tls_secure_channel_manager.secure_channel channel, byte[] data) throws IOException {
		return channel_crypto.send_encrypted(channel.socket(), data);
	}

	public int receive(tls_secure_channel_manager.secure_channel channel, byte[] buffer) throws IOException {
		return channel_crypto.receive_decrypted(channel.socket(), buffer);
	}

	public void close(tls_secure_channel_manager.secure_channel channel) {
		if (channel != null) {
			channel_manager.close(channel.channel_id());
		}
	}
}
