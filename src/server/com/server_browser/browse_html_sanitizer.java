/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Prepares fetched HTML for safe embedding in the in-app page frame.
 */
public final class browse_html_sanitizer {
	private static final Pattern CSP_META = Pattern.compile(
			"(?is)<meta[^>]+http-equiv\\s*=\\s*[\"']?Content-Security-Policy[\"']?[^>]*>");
	private static final Pattern FRAME_META = Pattern.compile(
			"(?is)<meta[^>]+http-equiv\\s*=\\s*[\"']?X-Frame-Options[\"']?[^>]*>");
	private static final Pattern BASE_TAG = Pattern.compile("(?is)<base\\b[^>]*>");

	private browse_html_sanitizer() {
	}

	public static byte[] for_embed(byte[] body, String content_type) {
		if (body == null || body.length == 0) {
			return new byte[0];
		}
		if (content_type == null || !content_type.toLowerCase().contains("text/html")) {
			return body;
		}
		String html = new String(body, StandardCharsets.UTF_8);
		html = CSP_META.matcher(html).replaceAll("");
		html = FRAME_META.matcher(html).replaceAll("");
		html = BASE_TAG.matcher(html).replaceAll("");
		return html.getBytes(StandardCharsets.UTF_8);
	}
}
