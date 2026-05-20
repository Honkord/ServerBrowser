/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Parses HTML, CSS, and JavaScript with DOM / pattern extraction and rewrites resource
 * references to the browse resource proxy (images, video, audio, fonts, files, etc.).
 */
public final class browse_dom_rewriter {
	private static final Pattern CSS_URL = Pattern.compile(
			"url\\(\\s*['\"]?([^'\"\\)]+)['\"]?\\s*\\)",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern CSS_IMPORT = Pattern.compile(
			"@import\\s+(?:url\\()?\\s*['\"]?([^'\"\\);]+)['\"]?",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern JS_QUOTED_HTTP = Pattern.compile(
			"([\"'])(https?:\\/\\/[^\"'\\s\\\\]+)\\1",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern JS_QUOTED_ASSET = Pattern.compile(
			"([\"'])((?:https?:)?\\/[^\"'\\s\\\\]+\\.(?:png|jpe?g|gif|webp|svg|ico|avif|mp4|webm|ogg|mp3|wav|woff2?|ttf|eot|css|js|mjs|json|pdf|zip|wasm))\\1",
			Pattern.CASE_INSENSITIVE);

	private browse_dom_rewriter() {
	}

	public static byte[] rewrite_embedded(
			byte[] body,
			String content_type,
			String base_url,
			String access_token) {
		if (body == null || body.length == 0 || access_token == null || access_token.isBlank()) {
			return body == null ? new byte[0] : body;
		}
		if (browse_binary_sniffer.is_binary_payload(body, content_type, base_url)) {
			return body;
		}
		URI base = browse_url_resolver.parse_base(base_url);
		if (base == null) {
			return body;
		}
		String text = new String(body, StandardCharsets.UTF_8);
		String type = content_type == null ? "" : content_type.toLowerCase(Locale.ROOT);
		String rewritten;
		if (type.contains("text/html") || looks_like_html(text)) {
			rewritten = rewrite_html(text, base, access_token);
		} else if (type.contains("text/css")) {
			rewritten = rewrite_css(text, base, access_token);
		} else if (type.contains("javascript") || type.contains("ecmascript")) {
			rewritten = rewrite_js(text, base, access_token);
		} else {
			return body;
		}
		return rewritten.getBytes(StandardCharsets.UTF_8);
	}

	static String rewrite_html(String html, URI base, String access_token) {
		Document document = Jsoup.parse(html, base.toString());
		document.outputSettings().prettyPrint(false);
		document.outputSettings().syntax(Document.OutputSettings.Syntax.html);

		for (Element element : document.select("img, audio, video, embed, input, track, source, script")) {
			rewrite_attr(element, "src", base, access_token);
			rewrite_attr(element, "poster", base, access_token);
			rewrite_attr(element, "data", base, access_token);
			rewrite_srcset(element, base, access_token);
		}
		for (Element element : document.select("image")) {
			rewrite_attr(element, "href", base, access_token);
			rewrite_attr(element, "xlink:href", base, access_token);
		}
		for (Element element : document.select("use")) {
			rewrite_attr(element, "href", base, access_token);
			rewrite_attr(element, "xlink:href", base, access_token);
		}
		for (Element element : document.select("link[href]")) {
			if (should_rewrite_link(element.attr("rel"))) {
				rewrite_attr(element, "href", base, access_token);
			}
		}
		for (Element element : document.select("object[data]")) {
			rewrite_attr(element, "data", base, access_token);
		}
		for (Element element : document.select("iframe[src]")) {
			rewrite_attr(element, "src", base, access_token);
		}
		for (Element element : document.select("meta[content]")) {
			String property = element.attr("property").toLowerCase(Locale.ROOT);
			String name = element.attr("name").toLowerCase(Locale.ROOT);
			if (property.contains("image") || name.contains("image") || "og:image".equals(property)) {
				rewrite_attr(element, "content", base, access_token);
			}
		}
		for (Element style : document.select("style")) {
			style.html(rewrite_css(style.html(), base, access_token));
		}
		for (Element element : document.select("[style]")) {
			String inline = element.attr("style");
			if (!inline.isBlank()) {
				element.attr("style", rewrite_css(inline, base, access_token));
			}
		}
		return document.outerHtml();
	}

	static String rewrite_css(String css, URI base, String access_token) {
		if (css == null || css.isBlank()) {
			return css == null ? "" : css;
		}
		String with_urls = rewrite_pattern(css, CSS_URL, base, access_token);
		return rewrite_pattern(with_urls, CSS_IMPORT, base, access_token);
	}

	static String rewrite_js(String js, URI base, String access_token) {
		if (js == null || js.isBlank()) {
			return js == null ? "" : js;
		}
		String with_http = rewrite_quoted_js(js, JS_QUOTED_HTTP, base, access_token);
		return rewrite_quoted_js(with_http, JS_QUOTED_ASSET, base, access_token);
	}

	private static String rewrite_quoted_js(
			String input,
			Pattern pattern,
			URI base,
			String access_token) {
		Matcher matcher = pattern.matcher(input);
		StringBuilder output = new StringBuilder();
		while (matcher.find()) {
			String reference = matcher.group(2);
			String replacement = matcher.group(0);
			Optional<String> absolute = browse_url_resolver.resolve_reference(reference, base);
			if (absolute.isPresent() && browse_allowed_target.is_allowed(absolute.get())) {
				replacement = matcher.group(0).replace(reference, browse_proxy_urls.resource_url(absolute.get(), access_token));
			}
			matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(output);
		return output.toString();
	}

	private static String rewrite_pattern(
			String input,
			Pattern pattern,
			URI base,
			String access_token) {
		Matcher matcher = pattern.matcher(input);
		StringBuilder output = new StringBuilder();
		while (matcher.find()) {
			String reference = matcher.group(1);
			String replacement = matcher.group(0);
			Optional<String> absolute = browse_url_resolver.resolve_reference(reference, base);
			if (absolute.isPresent() && browse_allowed_target.is_allowed(absolute.get())) {
				String proxied = browse_proxy_urls.resource_url(absolute.get(), access_token);
				replacement = matcher.group(0).replace(reference, proxied);
			}
			matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(output);
		return output.toString();
	}

	private static void rewrite_attr(Element element, String attribute, URI base, String access_token) {
		if (!element.hasAttr(attribute)) {
			return;
		}
		String value = element.attr(attribute);
		browse_url_resolver.resolve_reference(value, base)
				.filter(browse_allowed_target::is_allowed)
				.ifPresent(absolute -> element.attr(attribute, browse_proxy_urls.resource_url(absolute, access_token)));
	}

	private static void rewrite_srcset(Element element, URI base, String access_token) {
		if (!element.hasAttr("srcset")) {
			return;
		}
		element.attr("srcset", rewrite_srcset(element.attr("srcset"), base, access_token));
	}

	private static String rewrite_srcset(String srcset, URI base, String access_token) {
		StringBuilder output = new StringBuilder();
		for (String candidate : srcset.split(",")) {
			String trimmed = candidate.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			int space = trimmed.indexOf(' ');
			String url_part = space > 0 ? trimmed.substring(0, space).trim() : trimmed;
			String descriptor = space > 0 ? trimmed.substring(space) : "";
			Optional<String> absolute = browse_url_resolver.resolve_reference(url_part, base);
			if (absolute.isPresent() && browse_allowed_target.is_allowed(absolute.get())) {
				url_part = browse_proxy_urls.resource_url(absolute.get(), access_token);
			}
			if (!output.isEmpty()) {
				output.append(", ");
			}
			output.append(url_part);
			if (!descriptor.isEmpty()) {
				output.append(descriptor);
			}
		}
		return output.toString();
	}

	private static boolean should_rewrite_link(String rel) {
		if (rel == null || rel.isBlank()) {
			return true;
		}
		String lower = rel.toLowerCase(Locale.ROOT);
		return lower.contains("stylesheet")
				|| lower.contains("icon")
				|| lower.contains("preload")
				|| lower.contains("prefetch")
				|| lower.contains("modulepreload")
				|| lower.contains("manifest")
				|| lower.contains("apple-touch-icon");
	}

	static Pattern css_url_pattern() {
		return CSS_URL;
	}

	static Pattern css_import_pattern() {
		return CSS_IMPORT;
	}

	private static boolean looks_like_html(String text) {
		String trimmed = text.stripLeading();
		return trimmed.startsWith("<!") || trimmed.startsWith("<html") || trimmed.startsWith("<HTML");
	}
}
