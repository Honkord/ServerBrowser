/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

/**
 * Resource type hint for outbound {@code Accept} headers and fetch tuning.
 */
public enum browse_resource_kind {
	DOCUMENT,
	STYLESHEET,
	SCRIPT,
	IMAGE,
	FONT,
	MEDIA,
	ANY
}
