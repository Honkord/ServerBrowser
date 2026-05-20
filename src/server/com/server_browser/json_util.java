/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON helpers for API handlers.
 */
public final class json_util {
	private static final Pattern STRING_FIELD = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

	private json_util() {
	}

	public static String extract_string(String json, String field) {
		if (json == null || field == null) {
			return null;
		}
		Matcher matcher = STRING_FIELD.matcher(json);
		while (matcher.find()) {
			if (field.equals(matcher.group(1))) {
				return unescape(matcher.group(2));
			}
		}
		return null;
	}

	public static String json_object(String... key_values) {
		if (key_values.length % 2 != 0) {
			throw new IllegalArgumentException("key_values must be pairs");
		}
		StringBuilder json = new StringBuilder("{");
		for (int i = 0; i < key_values.length; i += 2) {
			if (i > 0) {
				json.append(',');
			}
			json.append('"').append(escape(key_values[i])).append("\":");
			String value = key_values[i + 1];
			if (value == null) {
				json.append("null");
			} else if ("true".equals(value) || "false".equals(value) || is_number(value)) {
				json.append(value);
			} else {
				json.append('"').append(escape(value)).append('"');
			}
		}
		json.append('}');
		return json.toString();
	}

	public static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}

	private static String unescape(String value) {
		return value
				.replace("\\\"", "\"")
				.replace("\\\\", "\\")
				.replace("\\n", "\n")
				.replace("\\r", "\r")
				.replace("\\t", "\t");
	}

	private static boolean is_number(String value) {
		return value.matches("-?\\d+(\\.\\d+)?");
	}

	public static byte[] utf8(String text) {
		return text.getBytes(StandardCharsets.UTF_8);
	}
}
