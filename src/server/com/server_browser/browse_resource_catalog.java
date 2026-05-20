/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Discovers subresource URLs from HTML/CSS with fetch priority hints.
 */
public final class browse_resource_catalog {
	public record catalog_entry(
			String url,
			browse_fetch_priority priority,
			browse_resource_kind kind,
			int order) {
	}

	private static final int MAX_HTML_RESOURCES = 96;
	private static final int MAX_CSS_URLS = 48;

	private browse_resource_catalog() {
	}

	public static List<catalog_entry> from_html(byte[] body, String base_url) {
		if (body == null || body.length == 0) {
			return List.of();
		}
		URI base = browse_url_resolver.parse_base(base_url);
		if (base == null) {
			return List.of();
		}
		String html = new String(body, StandardCharsets.UTF_8);
		Document document = Jsoup.parse(html, base.toString());
		Map<String, catalog_entry> unique = new LinkedHashMap<>();
		int order = 0;

		for (Element link : document.select("link[href]")) {
			String rel = link.attr("rel").toLowerCase(Locale.ROOT);
			browse_fetch_priority priority = link_priority(rel, link.attr("as"));
			browse_resource_kind kind = link_kind(rel, link.attr("as"));
			add(unique, link.attr("href"), base, priority, kind, order++);
		}

		int script_index = 0;
		for (Element script : document.select("script[src]")) {
			browse_fetch_priority priority = script_index < 8 ? browse_fetch_priority.HIGH : browse_fetch_priority.NORMAL;
			add(unique, script.attr("src"), base, priority, browse_resource_kind.SCRIPT, order++);
			script_index++;
		}

		int image_index = 0;
		for (Element image : document.select("img[src], img[srcset], source[src], source[srcset], video[src], video[poster], audio[src]")) {
			browse_fetch_priority priority = image_priority(image, image_index);
			browse_resource_kind kind = "video".equals(image.tagName()) || "audio".equals(image.tagName())
					? browse_resource_kind.MEDIA
					: browse_resource_kind.IMAGE;
			add(unique, image.attr("src"), base, priority, kind, order++);
			add_srcset(unique, image.attr("srcset"), base, priority, kind, order);
			if (image.hasAttr("poster")) {
				add(unique, image.attr("poster"), base, browse_fetch_priority.LOW, browse_resource_kind.IMAGE, order++);
			}
			image_index++;
		}

		for (Element style : document.select("style")) {
			for (catalog_entry entry : from_css(style.html(), base.toString(), browse_fetch_priority.HIGH, order)) {
				unique.putIfAbsent(entry.url(), entry);
				order = Math.max(order, entry.order() + 1);
			}
		}

		return unique.values().stream().limit(MAX_HTML_RESOURCES).toList();
	}

	public static List<catalog_entry> from_css(String css, String base_url, browse_fetch_priority priority, int start_order) {
		if (css == null || css.isBlank()) {
			return List.of();
		}
		URI base = browse_url_resolver.parse_base(base_url);
		if (base == null) {
			return List.of();
		}
		Map<String, catalog_entry> unique = new LinkedHashMap<>();
		int order = start_order;
		Matcher url_matcher = browse_dom_rewriter.css_url_pattern().matcher(css);
		while (url_matcher.find()) {
			add(unique, url_matcher.group(1), base, priority, browse_resource_kind.ANY, order++);
		}
		Matcher import_matcher = browse_dom_rewriter.css_import_pattern().matcher(css);
		while (import_matcher.find()) {
			add(unique, import_matcher.group(1), base, browse_fetch_priority.CRITICAL, browse_resource_kind.STYLESHEET, order++);
		}
		return unique.values().stream().limit(MAX_CSS_URLS).toList();
	}

	private static browse_fetch_priority link_priority(String rel, String as) {
		String as_lower = as == null ? "" : as.toLowerCase(Locale.ROOT);
		if (rel.contains("stylesheet")) {
			return browse_fetch_priority.CRITICAL;
		}
		if (rel.contains("preload") || rel.contains("modulepreload")) {
			if (as_lower.contains("style") || as_lower.contains("font")) {
				return browse_fetch_priority.CRITICAL;
			}
			if (as_lower.contains("script")) {
				return browse_fetch_priority.HIGH;
			}
			return browse_fetch_priority.HIGH;
		}
		if (rel.contains("icon") || rel.contains("manifest")) {
			return browse_fetch_priority.LOW;
		}
		return browse_fetch_priority.NORMAL;
	}

	private static browse_resource_kind link_kind(String rel, String as) {
		String as_lower = as == null ? "" : as.toLowerCase(Locale.ROOT);
		if (rel.contains("stylesheet") || as_lower.contains("style")) {
			return browse_resource_kind.STYLESHEET;
		}
		if (rel.contains("modulepreload") || as_lower.contains("script")) {
			return browse_resource_kind.SCRIPT;
		}
		if (as_lower.contains("font")) {
			return browse_resource_kind.FONT;
		}
		if (as_lower.contains("image")) {
			return browse_resource_kind.IMAGE;
		}
		return browse_resource_kind.ANY;
	}

	private static browse_fetch_priority image_priority(Element image, int index) {
		if ("high".equalsIgnoreCase(image.attr("fetchpriority")) || index < 6) {
			return browse_fetch_priority.NORMAL;
		}
		return browse_fetch_priority.LOW;
	}

	private static void add_srcset(
			Map<String, catalog_entry> unique,
			String srcset,
			URI base,
			browse_fetch_priority priority,
			browse_resource_kind kind,
			int order) {
		if (srcset == null || srcset.isBlank()) {
			return;
		}
		for (String candidate : srcset.split(",")) {
			String trimmed = candidate.trim();
			int space = trimmed.indexOf(' ');
			String url_part = space > 0 ? trimmed.substring(0, space).trim() : trimmed;
			add(unique, url_part, base, priority, kind, order);
		}
	}

	private static void add(
			Map<String, catalog_entry> unique,
			String reference,
			URI base,
			browse_fetch_priority priority,
			browse_resource_kind kind,
			int order) {
		browse_url_resolver.resolve_reference(reference, base)
				.filter(browse_allowed_target::is_allowed)
				.ifPresent(absolute -> unique.putIfAbsent(absolute, new catalog_entry(absolute, priority, kind, order)));
	}
}
