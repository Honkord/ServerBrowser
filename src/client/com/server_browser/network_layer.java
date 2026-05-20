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
import java.util.Optional;
import javax.net.ssl.SSLSocket;

/**
 * Network layer facade composing DNS resolution, TCP client I/O, and TLS.
 */
public final class network_layer {
	public enum transport_mode {
		HTTP,
		HTTPS
	}

	public sealed interface network_connection permits plain_tcp_connection, tls_connection {
		String hostname();

		int port();

		transport_mode mode();
	}

	public record plain_tcp_connection(Socket socket, String hostname, int port) implements network_connection {
		@Override
		public transport_mode mode() {
			return transport_mode.HTTP;
		}
	}

	public record tls_connection(
			tls_secure_channel_manager.secure_channel channel,
			String hostname,
			int port) implements network_connection {
		@Override
		public transport_mode mode() {
			return transport_mode.HTTPS;
		}
	}

	private final dns_resolver dns;
	private final tcp_client_engine tcp;
	private final tls_engine tls;
	private final int connect_timeout_ms;

	public network_layer() {
		this(new dns_resolver(), new tcp_client_engine(), new tls_engine(), 10_000);
	}

	public network_layer(
			dns_resolver dns,
			tcp_client_engine tcp,
			tls_engine tls,
			int connect_timeout_ms) {
		this.dns = dns;
		this.tcp = tcp;
		this.tls = tls;
		this.connect_timeout_ms = connect_timeout_ms;
	}

	public dns_resolver dns() {
		return dns;
	}

	public tcp_client_engine tcp() {
		return tcp;
	}

	public tls_engine tls() {
		return tls;
	}

	public Optional<network_connection> connect(String hostname, int port, transport_mode mode) {
		if (hostname == null || hostname.isBlank()) {
			return Optional.empty();
		}
		Optional<InetAddress> address = dns.resolve_first(hostname);
		if (address.isEmpty()) {
			return Optional.empty();
		}
		if (mode == transport_mode.HTTPS) {
			return connect_tls(hostname, port);
		}
		return connect_tcp(address.get(), hostname, port);
	}

	private Optional<network_connection> connect_tcp(InetAddress address, String hostname, int port) {
		try {
			Socket socket = tcp.open_connection(address, port, connect_timeout_ms);
			return Optional.of(new plain_tcp_connection(socket, hostname, port));
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	private Optional<network_connection> connect_tls(String hostname, int port) {
		return tls.open_secure_channel(hostname, port, connect_timeout_ms)
				.map(channel -> new tls_connection(channel, hostname, port));
	}

	public int send(network_connection connection, byte[] data) throws IOException {
		if (connection instanceof plain_tcp_connection plain) {
			return tcp.transmit(plain.socket(), data);
		}
		if (connection instanceof tls_connection secure) {
			return tls.send(secure.channel(), data);
		}
		throw new IllegalArgumentException("unsupported connection type");
	}

	public int receive(network_connection connection, byte[] buffer) throws IOException {
		if (connection instanceof plain_tcp_connection plain) {
			return tcp.receive(plain.socket(), buffer);
		}
		if (connection instanceof tls_connection secure) {
			return tls.receive(secure.channel(), buffer);
		}
		throw new IllegalArgumentException("unsupported connection type");
	}

	public void close(network_connection connection) {
		if (connection instanceof plain_tcp_connection plain) {
			tcp.close(plain.socket());
		} else if (connection instanceof tls_connection secure) {
			tls.close(secure.channel());
		}
	}
}
