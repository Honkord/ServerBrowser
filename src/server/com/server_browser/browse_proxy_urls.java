/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds same-origin URLs for proxied page subresources (images, media, CSS, JS, files).
 */
public final class browse_proxy_urls {
	public static final String RESOURCE_PATH = "/api/browse/resource";

	private browse_proxy_urls() {
	}

	public static String resource_url(String absolute_url, String access_token) {
		if (absolute_url == null || absolute_url.isBlank() || access_token == null || access_token.isBlank()) {
			return absolute_url;
		}
		return RESOURCE_PATH
				+ "?url="
				+ URLEncoder.encode(absolute_url, StandardCharsets.UTF_8)
				+ "&access_token="
				+ URLEncoder.encode(access_token, StandardCharsets.UTF_8);
	}
}
