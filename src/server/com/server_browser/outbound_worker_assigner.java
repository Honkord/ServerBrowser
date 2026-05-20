/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assigns an outbound worker for handling a proxied request.
 */
public final class outbound_worker_assigner {
	public record outbound_worker(int worker_id) {
	}

	private final int worker_count;
	private final AtomicInteger round_robin = new AtomicInteger(0);

	public outbound_worker_assigner(int worker_count) {
		if (worker_count < 1) {
			throw new IllegalArgumentException("worker_count must be at least 1");
		}
		this.worker_count = worker_count;
	}

	public outbound_worker_assigner() {
		this(Runtime.getRuntime().availableProcessors());
	}

	public outbound_worker assign(target_website_resolver.route_target target) {
		if (target == null) {
			throw new IllegalArgumentException("target is required");
		}
		int worker_id = (target.host().hashCode() & 0x7fffffff) % worker_count;
		return new outbound_worker(worker_id);
	}

	public outbound_worker assign_next() {
		int worker_id = Math.floorMod(round_robin.getAndIncrement(), worker_count);
		return new outbound_worker(worker_id);
	}

	public int worker_count() {
		return worker_count;
	}
}
