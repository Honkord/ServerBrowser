/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.util.Locale;

/**
 * Detects browser-decodable media vs opaque binary payloads.
 */
public final class browse_binary_sniffer {
	public enum content_class {
		TEXT,
		MEDIA_IMAGE,
		MEDIA_VIDEO,
		MEDIA_AUDIO,
		MEDIA_SVG,
		DOWNLOAD
	}

	private browse_binary_sniffer() {
	}

	public static content_class classify(byte[] body, String content_type, String url) {
		String type = content_type == null ? "" : content_type.toLowerCase(Locale.ROOT).split(";")[0].trim();
		if (type.startsWith("text/") || type.contains("json") || type.contains("javascript") || type.contains("xml")) {
			return content_class.TEXT;
		}
		if (type.startsWith("image/svg")) {
			return content_class.MEDIA_SVG;
		}
		if (type.startsWith("image/")) {
			return content_class.MEDIA_IMAGE;
		}
		if (type.startsWith("video/")) {
			return content_class.MEDIA_VIDEO;
		}
		if (type.startsWith("audio/")) {
			return content_class.MEDIA_AUDIO;
		}
		if (body != null && body.length >= 4) {
			content_class magic = classify_magic(body);
			if (magic != null) {
				return magic;
			}
		}
		if (type.startsWith("application/pdf")
				|| type.startsWith("application/zip")
				|| type.startsWith("application/octet-stream")
				|| type.startsWith("application/x-")
				|| type.startsWith("font/")) {
			return content_class.DOWNLOAD;
		}
		String path = url_path(url).toLowerCase(Locale.ROOT);
		if (path.endsWith(".png")
				|| path.endsWith(".jpg")
				|| path.endsWith(".jpeg")
				|| path.endsWith(".gif")
				|| path.endsWith(".webp")
				|| path.endsWith(".avif")
				|| path.endsWith(".ico")
				|| path.endsWith(".bmp")) {
			return content_class.MEDIA_IMAGE;
		}
		if (path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".ogg") || path.endsWith(".mov")) {
			return content_class.MEDIA_VIDEO;
		}
		if (path.endsWith(".mp3") || path.endsWith(".wav") || path.endsWith(".m4a") || path.endsWith(".aac")) {
			return content_class.MEDIA_AUDIO;
		}
		if (path.endsWith(".svg")) {
			return content_class.MEDIA_SVG;
		}
		if (is_textual_body(body)) {
			return content_class.TEXT;
		}
		return content_class.DOWNLOAD;
	}

	public static boolean is_binary_payload(byte[] body, String content_type, String url) {
		return classify(body, content_type, url) != content_class.TEXT;
	}

	public static boolean is_textual_body(byte[] body) {
		if (body == null || body.length == 0) {
			return true;
		}
		int sample = Math.min(body.length, 512);
		for (int index = 0; index < sample; index++) {
			byte value = body[index];
			if (value == 9 || value == 10 || value == 13) {
				continue;
			}
			if (value < 32 || value == 127) {
				return false;
			}
		}
		return true;
	}

	private static content_class classify_magic(byte[] body) {
		if (body[0] == (byte) 0x89 && body[1] == 0x50 && body[2] == 0x4E && body[3] == 0x47) {
			return content_class.MEDIA_IMAGE;
		}
		if (body[0] == (byte) 0xFF && body[1] == (byte) 0xD8) {
			return content_class.MEDIA_IMAGE;
		}
		if (body[0] == 0x47 && body[1] == 0x49 && body[2] == 0x46) {
			return content_class.MEDIA_IMAGE;
		}
		if (body[0] == 0x52 && body[1] == 0x49 && body[2] == 0x46 && body[3] == 0x46 && body.length > 11) {
			String riff = new String(body, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
			if ("WEBP".equals(riff)) {
				return content_class.MEDIA_IMAGE;
			}
			if ("WAVE".equals(riff)) {
				return content_class.MEDIA_AUDIO;
			}
		}
		if (body.length > 11 && body[4] == 0x66 && body[5] == 0x74 && body[6] == 0x79 && body[7] == 0x70) {
			return content_class.MEDIA_VIDEO;
		}
		if (body[0] == 0x25 && body[1] == 0x50 && body[2] == 0x44 && body[3] == 0x46) {
			return content_class.DOWNLOAD;
		}
		if (body[0] == 0x50 && body[1] == 0x4B) {
			return content_class.DOWNLOAD;
		}
		return null;
	}

	private static String url_path(String url) {
		try {
			String path = java.net.URI.create(url).getPath();
			return path == null ? "" : path;
		} catch (Exception e) {
			return "";
		}
	}
}
