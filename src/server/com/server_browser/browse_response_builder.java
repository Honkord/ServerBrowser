/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.util.Base64;

/**
 * Builds browse API JSON without escaping large HTML bodies as UTF-8 strings.
 */
public final class browse_response_builder {
	private browse_response_builder() {
	}

	public static String build(fast_page_fetcher.fetch_result result) {
		String encoded = Base64.getEncoder().encodeToString(result.body());
		StringBuilder json = new StringBuilder(256 + encoded.length());
		json.append("{\"statusCode\":").append(result.status_code());
		json.append(",\"contentType\":\"").append(json_util.escape(result.content_type())).append('"');
		json.append(",\"resolvedUrl\":\"").append(json_util.escape(result.resolved_url())).append('"');
		json.append(",\"bodyEncoding\":\"base64\"");
		json.append(",\"bodyBase64\":\"").append(encoded).append("\"}");
		return json.toString();
	}
}
