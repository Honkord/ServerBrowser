/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parses and serializes HTTP cookies for proxied requests.
 */
public final class http_cookie_handling {
	public Map<String, String> parse_set_cookie_headers(List<String> set_cookie_values) {
		Map<String, String> jar = new LinkedHashMap<>();
		if (set_cookie_values == null) {
			return jar;
		}
		for (String header : set_cookie_values) {
			if (header == null || header.isBlank()) {
				continue;
			}
			String pair = header.split(";", 2)[0].trim();
			int separator = pair.indexOf('=');
			if (separator <= 0) {
				continue;
			}
			String name = pair.substring(0, separator).trim();
			String value = pair.substring(separator + 1).trim();
			jar.put(name, value);
		}
		return jar;
	}

	public void store_cookies(Map<String, String> jar, Map<String, String> new_cookies) {
		if (new_cookies == null) {
			return;
		}
		jar.putAll(new_cookies);
	}

	public String build_cookie_header(Map<String, String> jar) {
		if (jar == null || jar.isEmpty()) {
			return "";
		}
		return jar.entrySet().stream()
				.map(entry -> entry.getKey() + "=" + entry.getValue())
				.collect(Collectors.joining("; "));
	}

	public void apply_cookie_header(Map<String, String> request_headers, Map<String, String> jar) {
		String cookie_header = build_cookie_header(jar);
		if (!cookie_header.isEmpty()) {
			request_headers.put("Cookie", cookie_header);
		}
	}
}
