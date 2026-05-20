/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Normalizes and queries HTTP header fields.
 */
public final class http_header_processing {
	public Map<String, List<String>> parse_header_block(List<String> header_lines) {
		Map<String, List<String>> headers = new LinkedHashMap<>();
		if (header_lines == null) {
			return headers;
		}
		for (String line : header_lines) {
			if (line == null || line.isBlank()) {
				continue;
			}
			int separator = line.indexOf(':');
			if (separator <= 0) {
				continue;
			}
			String name = line.substring(0, separator).trim();
			String value = line.substring(separator + 1).trim();
			add_header(headers, name, value);
		}
		return headers;
	}

	public void add_header(Map<String, List<String>> headers, String name, String value) {
		String key = normalize_name(name);
		headers.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
	}

	public void merge_headers(Map<String, List<String>> target, Map<String, List<String>> source) {
		if (source == null) {
			return;
		}
		for (Map.Entry<String, List<String>> entry : source.entrySet()) {
			for (String value : entry.getValue()) {
				add_header(target, entry.getKey(), value);
			}
		}
	}

	public Optional<String> first_value(Map<String, List<String>> headers, String name) {
		List<String> values = headers.get(normalize_name(name));
		if (values == null || values.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(values.get(0));
	}

	public List<String> all_values(Map<String, List<String>> headers, String name) {
		List<String> values = headers.get(normalize_name(name));
		if (values == null) {
			return List.of();
		}
		return List.copyOf(values);
	}

	public Map<String, String> to_single_value_map(Map<String, List<String>> headers) {
		Map<String, String> flattened = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				flattened.put(entry.getKey(), entry.getValue().get(0));
			}
		}
		return flattened;
	}

	private static String normalize_name(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}
}
