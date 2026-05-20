/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLSocket;

/**
 * Manages secure TLS channel lifecycle and lookup.
 */
public final class tls_secure_channel_manager {
	public record secure_channel(String channel_id, SSLSocket socket, String hostname) {
	}

	private final Map<String, secure_channel> channels = new ConcurrentHashMap<>();

	public secure_channel register(SSLSocket socket, String hostname) {
		secure_channel channel = new secure_channel(UUID.randomUUID().toString(), socket, hostname);
		channels.put(channel.channel_id(), channel);
		return channel;
	}

	public Optional<secure_channel> get(String channel_id) {
		return Optional.ofNullable(channels.get(channel_id));
	}

	public void close(String channel_id) {
		secure_channel channel = channels.remove(channel_id);
		if (channel == null) {
			return;
		}
		try {
			channel.socket().close();
		} catch (IOException ignored) {
		}
	}

	public int active_channel_count() {
		return channels.size();
	}
}
