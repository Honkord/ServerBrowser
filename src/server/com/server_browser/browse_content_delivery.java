/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import com.sun.net.httpserver.Headers;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Sets HTTP headers for inline media vs downloadable files.
 */
public final class browse_content_delivery {
	private browse_content_delivery() {
	}

	public static void apply_resource_headers(
			Headers headers,
			byte[] body,
			String content_type,
			String resolved_url) {
		browse_binary_sniffer.content_class kind =
				browse_binary_sniffer.classify(body, content_type, resolved_url);
		String type = normalize_type(content_type, kind, resolved_url);
		headers.set("Content-Type", type);
		headers.set("X-Browse-Content-Class", kind.name().toLowerCase());
		headers.set("Access-Control-Expose-Headers", "X-Browse-Content-Class, Content-Disposition");
		String filename = filename_from_url(resolved_url);
		if (kind == browse_binary_sniffer.content_class.DOWNLOAD) {
			headers.set("Content-Disposition", "attachment; filename=\"" + escape_filename(filename) + "\"");
		} else if (kind != browse_binary_sniffer.content_class.TEXT) {
			headers.set("Content-Disposition", "inline; filename=\"" + escape_filename(filename) + "\"");
		}
	}

	public static byte[] prepare_body(
			byte[] body,
			String content_type,
			String resolved_url,
			String access_token) {
		if (browse_binary_sniffer.is_binary_payload(body, content_type, resolved_url)) {
			return body == null ? new byte[0] : body;
		}
		return browse_dom_rewriter.rewrite_embedded(body, content_type, resolved_url, access_token);
	}

	private static String normalize_type(
			String content_type,
			browse_binary_sniffer.content_class kind,
			String url) {
		if (content_type != null && !content_type.isBlank() && !content_type.contains("octet-stream")) {
			return content_type.split(";")[0].trim();
		}
		return switch (kind) {
			case MEDIA_IMAGE -> "image/png";
			case MEDIA_VIDEO -> "video/mp4";
			case MEDIA_AUDIO -> "audio/mpeg";
			case MEDIA_SVG -> "image/svg+xml";
			case DOWNLOAD -> guess_download_type(url);
			default -> "application/octet-stream";
		};
	}

	private static String guess_download_type(String url) {
		String path = url == null ? "" : url.toLowerCase();
		if (path.endsWith(".pdf")) {
			return "application/pdf";
		}
		if (path.endsWith(".zip")) {
			return "application/zip";
		}
		if (path.endsWith(".json")) {
			return "application/json";
		}
		return "application/octet-stream";
	}

	private static String filename_from_url(String url) {
		try {
			String path = URI.create(url).getPath();
			if (path == null || path.isBlank() || path.endsWith("/")) {
				return "download";
			}
			int slash = path.lastIndexOf('/');
			return slash >= 0 ? path.substring(slash + 1) : path;
		} catch (Exception e) {
			return "download";
		}
	}

	private static String escape_filename(String filename) {
		return filename.replace("\"", "").replace("\n", "").replace("\r", "");
	}
}
