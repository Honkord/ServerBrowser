/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies routing policies (allow/deny) before a request is forwarded.
 */
public final class routing_policy_engine {
	public enum routing_decision {
		ALLOW,
		DENY
	}

	private volatile boolean require_https;
	private final Set<String> blocked_hosts = ConcurrentHashMap.newKeySet();

	public routing_policy_engine() {
	}

	public void set_require_https(boolean require_https) {
		this.require_https = require_https;
	}

	public void block_host(String host) {
		if (host != null && !host.isBlank()) {
			blocked_hosts.add(host.toLowerCase());
		}
	}

	public void unblock_host(String host) {
		if (host != null) {
			blocked_hosts.remove(host.toLowerCase());
		}
	}

	public Set<String> blocked_hosts() {
		return Collections.unmodifiableSet(blocked_hosts);
	}

	public routing_decision apply_policies(
			target_website_resolver.route_target target,
			protocol_selector.protocol selected_protocol) {
		if (target == null || target.host() == null || target.host().isBlank()) {
			return routing_decision.DENY;
		}
		if (blocked_hosts.contains(target.host())) {
			return routing_decision.DENY;
		}
		if (is_local_loopback(target.host())) {
			return routing_decision.DENY;
		}
		if (require_https && selected_protocol != protocol_selector.protocol.HTTPS) {
			return routing_decision.DENY;
		}
		return routing_decision.ALLOW;
	}

	private static boolean is_local_loopback(String host) {
		return host.equals("localhost")
				|| host.equals("127.0.0.1")
				|| host.equals("::1")
				|| host.endsWith(".localhost");
	}
}
