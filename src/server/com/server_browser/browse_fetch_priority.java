/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

/**
 * Load order for subresources discovered during browse (lower loads first).
 */
public enum browse_fetch_priority {
	CRITICAL(0),
	HIGH(1),
	NORMAL(2),
	LOW(3);

	private final int rank;

	browse_fetch_priority(int rank) {
		this.rank = rank;
	}

	public int rank() {
		return rank;
	}
}
