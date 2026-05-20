/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.util.Optional;

/**
 * Request router facade: resolves target, selects protocol, applies policies, assigns worker.
 */
public final class request_router {
	public record routed_request(
			target_website_resolver.route_target target,
			protocol_selector.protocol protocol,
			outbound_worker_assigner.outbound_worker worker) {
	}

	private final target_website_resolver target_resolver;
	private final protocol_selector protocol_selector;
	private final routing_policy_engine routing_policies;
	private final outbound_worker_assigner worker_assigner;

	public request_router() {
		this(
				new target_website_resolver(),
				new protocol_selector(),
				new routing_policy_engine(),
				new outbound_worker_assigner());
	}

	public request_router(
			target_website_resolver target_resolver,
			protocol_selector protocol_selector,
			routing_policy_engine routing_policies,
			outbound_worker_assigner worker_assigner) {
		this.target_resolver = target_resolver;
		this.protocol_selector = protocol_selector;
		this.routing_policies = routing_policies;
		this.worker_assigner = worker_assigner;
	}

	public target_website_resolver targets() {
		return target_resolver;
	}

	public protocol_selector protocols() {
		return protocol_selector;
	}

	public routing_policy_engine policies() {
		return routing_policies;
	}

	public outbound_worker_assigner workers() {
		return worker_assigner;
	}

	/**
	 * Full routing pipeline for an inbound request URI.
	 */
	public Optional<routed_request> route(String request_uri) {
		Optional<target_website_resolver.route_target> target = target_resolver.resolve_target(request_uri);
		if (target.isEmpty()) {
			return Optional.empty();
		}
		protocol_selector.protocol protocol = protocol_selector.select(target.get());
		if (routing_policies.apply_policies(target.get(), protocol) == routing_policy_engine.routing_decision.DENY) {
			return Optional.empty();
		}
		outbound_worker_assigner.outbound_worker worker = worker_assigner.assign(target.get());
		return Optional.of(new routed_request(target.get(), protocol, worker));
	}
}
