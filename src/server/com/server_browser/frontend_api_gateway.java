/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Frontend API gateway: auth, session validation, browse, DNS phonebook, and security.
 */
public final class frontend_api_gateway {
	private final server_runtime runtime;
	private final fast_page_fetcher page_fetcher;
	private final browse_resource_cache resource_cache;
	private final browse_prefetch_service prefetch_service;
	private final oauth_service oauth_service;
	private final request_integrity_seal integrity_seal;

	public frontend_api_gateway(server_runtime runtime) {
		this.runtime = runtime;
		this.page_fetcher = new fast_page_fetcher();
		this.resource_cache = new browse_resource_cache();
		this.prefetch_service = new browse_prefetch_service(page_fetcher, resource_cache);
		this.oauth_service = new oauth_service(runtime.database(), runtime.authentication());
		this.integrity_seal = runtime.integrity_seal();
	}

	public void register(HttpServer server) {
		secure_connection_pipeline pipeline = runtime.secure_pipeline();
		server.createContext("/api/auth/register", secure(new register_handler(), pipeline));
		server.createContext("/api/auth/login", secure(new login_handler(), pipeline));
		server.createContext("/api/auth/session", secure(new session_handler(), pipeline));
		server.createContext("/api/auth/oauth", secure(new oauth_handler(), pipeline));
		server.createContext("/api/browse", secure(new browse_handler(), pipeline));
		server.createContext("/api/browse/resource", secure(new browse_resource_handler(), pipeline));
		server.createContext("/api/security/pin", secure(new security_pin_handler(), pipeline));
		server.createContext("/api/dns/phonebook", secure(new dns_phonebook_handler(), pipeline));
		server.createContext("/api/dns/resolve", secure(new dns_resolve_handler(), pipeline));
	}

	private HttpHandler secure(HttpHandler handler, secure_connection_pipeline pipeline) {
		return new connection_security_filter(handler, pipeline);
	}

	private String read_body(HttpExchange exchange) throws IOException {
		return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
	}

	private void write_html(HttpExchange exchange, int status, String html) throws IOException {
		byte[] payload = html.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
		exchange.sendResponseHeaders(status, payload.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(payload);
		}
	}

	private void write_json(HttpExchange exchange, int status, String json) throws IOException {
		byte[] payload = json_util.utf8(json);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		exchange.sendResponseHeaders(status, payload.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(payload);
		}
	}

	private void write_browse(HttpExchange exchange, fast_page_fetcher.fetch_result result, String access_token)
			throws IOException {
		byte[] body = result.body() == null ? new byte[0] : result.body();
		String content_type = result.content_type();
		var headers = exchange.getResponseHeaders();
		if (!browse_binary_sniffer.is_binary_payload(body, content_type, result.resolved_url())) {
			if (is_html(content_type, body)) {
				prefetch_service.prefetch_html(body, result.resolved_url());
			}
			body = browse_html_sanitizer.for_embed(body, content_type);
			body = browse_dom_rewriter.rewrite_embedded(body, content_type, result.resolved_url(), access_token);
			headers.set("Content-Type", content_type);
			headers.set("X-Browse-Content-Class", "text");
		} else {
			browse_content_delivery.apply_resource_headers(headers, body, content_type, result.resolved_url());
		}
		headers.set("X-Browse-Status", String.valueOf(result.status_code()));
		headers.set("X-Browse-Resolved-Url", result.resolved_url());
		headers.set(
				"Access-Control-Expose-Headers",
				"X-Browse-Status, X-Browse-Resolved-Url, X-Browse-Content-Class, Content-Disposition");
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(body);
		}
	}

	private void write_resource(HttpExchange exchange, fast_page_fetcher.fetch_result result, String access_token)
			throws IOException {
		write_resource(exchange, result, access_token, false);
	}

	private void write_cached_resource(
			HttpExchange exchange,
			fast_page_fetcher.fetch_result result,
			String access_token)
			throws IOException {
		write_resource(exchange, result, access_token, true);
	}

	private void write_resource(
			HttpExchange exchange,
			fast_page_fetcher.fetch_result result,
			String access_token,
			boolean from_cache)
			throws IOException {
		byte[] body = result.body() == null ? new byte[0] : result.body();
		String content_type = result.content_type();
		if (is_css(content_type) && browse_binary_sniffer.is_textual_body(body)) {
			prefetch_service.prefetch_css(new String(body, StandardCharsets.UTF_8), result.resolved_url());
		}
		body = browse_content_delivery.prepare_body(body, content_type, result.resolved_url(), access_token);
		var headers = exchange.getResponseHeaders();
		browse_content_delivery.apply_resource_headers(headers, body, content_type, result.resolved_url());
		headers.set("Cache-Control", "private, max-age=900");
		headers.set("X-Browse-Cache", from_cache ? "hit" : "miss");
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(body);
		}
	}

	private Optional<fast_page_fetcher.fetch_result> load_resource(String url, browse_resource_kind kind) {
		Optional<fast_page_fetcher.fetch_result> cached = resource_cache.await(url, Duration.ofMillis(900));
		if (cached.isPresent()) {
			return cached;
		}
		if (!resource_cache.reserve(url)) {
			return resource_cache.await(url, Duration.ofMillis(900));
		}
		Optional<fast_page_fetcher.fetch_result> fetched = page_fetcher.fetch(URI.create(url), kind);
		if (fetched.isPresent()) {
			resource_cache.put(url, fetched.get());
			return fetched;
		}
		resource_cache.release_in_flight(url);
		return Optional.empty();
	}

	private static boolean is_html(String content_type, byte[] body) {
		if (content_type != null && content_type.toLowerCase().contains("text/html")) {
			return true;
		}
		if (body == null || body.length == 0) {
			return false;
		}
		String start = new String(body, 0, Math.min(body.length, 32), StandardCharsets.UTF_8).stripLeading();
		return start.startsWith("<!") || start.startsWith("<html") || start.startsWith("<HTML");
	}

	private static boolean is_css(String content_type) {
		return content_type != null && content_type.toLowerCase().contains("text/css");
	}

	private static browse_resource_kind kind_for_url(String url) {
		String path;
		try {
			path = URI.create(url).getPath();
		} catch (Exception e) {
			return browse_resource_kind.ANY;
		}
		if (path == null) {
			return browse_resource_kind.ANY;
		}
		String lower = path.toLowerCase();
		if (lower.endsWith(".css")) {
			return browse_resource_kind.STYLESHEET;
		}
		if (lower.endsWith(".js") || lower.endsWith(".mjs")) {
			return browse_resource_kind.SCRIPT;
		}
		if (lower.endsWith(".woff2") || lower.endsWith(".woff") || lower.endsWith(".ttf")) {
			return browse_resource_kind.FONT;
		}
		if (lower.endsWith(".png")
				|| lower.endsWith(".jpg")
				|| lower.endsWith(".jpeg")
				|| lower.endsWith(".gif")
				|| lower.endsWith(".webp")
				|| lower.endsWith(".svg")
				|| lower.endsWith(".ico")) {
			return browse_resource_kind.IMAGE;
		}
		if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mp3")) {
			return browse_resource_kind.MEDIA;
		}
		return browse_resource_kind.ANY;
	}

	private void log_browse_async(String token, String target_url) {
		new Thread(() -> {
			try {
				String user_id = runtime.authentication().sessions()
						.validate_session(token)
						.map(session_manager.session_record::user_id)
						.orElse(null);
				runtime.database().audit_logs().insert(user_id, "browse", "Fetched " + target_url);
			} catch (Exception ignored) {
			}
		}, "browse-audit").start();
	}

	private Optional<String> bearer_token(HttpExchange exchange) {
		String header = exchange.getRequestHeaders().getFirst("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return Optional.of(header.substring("Bearer ".length()).trim());
		}
		return Optional.empty();
	}

	private Optional<String> browse_access_token(HttpExchange exchange) {
		Optional<String> bearer = bearer_token(exchange);
		if (bearer.isPresent()) {
			return bearer;
		}
		return query_param(exchange, "access_token");
	}

	private static Optional<String> query_param(HttpExchange exchange, String name) {
		String query = exchange.getRequestURI().getRawQuery();
		if (query == null || query.isBlank()) {
			return Optional.empty();
		}
		for (String part : query.split("&")) {
			int eq = part.indexOf('=');
			if (eq > 0 && name.equals(part.substring(0, eq))) {
				return Optional.of(java.net.URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
			}
		}
		return Optional.empty();
	}

	private boolean verify_integrity(HttpExchange exchange, String token, byte[] body) {
		String timestamp = exchange.getRequestHeaders().getFirst(request_integrity_seal.HEADER_TIMESTAMP);
		String seal = exchange.getRequestHeaders().getFirst(request_integrity_seal.HEADER_SEAL);
		String path = exchange.getRequestURI().getPath();
		return integrity_seal.verify(token, timestamp, exchange.getRequestMethod(), path, body, seal);
	}

	private String resolve_target_url(String url) throws Exception {
		Optional<server_browser_org_phonebook.phonebook_resolution> phonebook =
				runtime.phonebook().resolve_host(url);
		if (phonebook.isPresent() && phonebook.get().resolved_url().isPresent()) {
			return phonebook.get().resolved_url().get();
		}
		return url;
	}

	private final class register_handler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				write_json(exchange, 405, json_util.json_object("error", "Method not allowed"));
				return;
			}
			String body = read_body(exchange);
			String username = json_util.extract_string(body, "username");
			String password = json_util.extract_string(body, "password");
			if (username == null || password == null) {
				write_json(exchange, 400, json_util.json_object("error", "username and password required"));
				return;
			}
			try {
				authentication_subsystem.registered_user user =
						runtime.authentication().register_user(username, password.toCharArray());
				write_json(
						exchange,
						201,
						json_util.json_object(
								"userId", user.user_id(),
								"username", user.username(),
								"message", "registered"));
			} catch (IllegalArgumentException e) {
				write_json(
						exchange,
						409,
						json_util.json_object("error", e.getMessage(), "code", "username_taken"));
			} catch (IllegalStateException e) {
				write_json(exchange, 500, json_util.json_object("error", "Could not create account. Try again."));
			}
		}
	}

	private final class login_handler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				write_json(exchange, 405, json_util.json_object("error", "Method not allowed"));
				return;
			}
			String body = read_body(exchange);
			String username = json_util.extract_string(body, "username");
			String password = json_util.extract_string(body, "password");
			if (username == null || password == null) {
				write_json(exchange, 400, json_util.json_object("error", "username and password required"));
				return;
			}
			authentication_subsystem.login_result result = runtime.authentication()
					.authenticate(username, password.toCharArray());
			if (result.session().isEmpty()) {
				String message = switch (result.outcome()) {
					case OAUTH_ACCOUNT -> "This account uses corporate sign-in. Use Google, Microsoft, or Apple above.";
					case NOT_FOUND -> "No account with that username. Create an account or use corporate sign-in.";
					default -> "Invalid credentials";
				};
				write_json(exchange, 401, json_util.json_object("error", message));
				return;
			}
			session_manager.session_record session = result.session().get();
			write_json(
					exchange,
					200,
					json_util.json_object(
							"accessToken", session.access_token(),
							"refreshToken", session.refresh_token(),
							"expiresAt", session.expires_at().toString(),
							"username", username));
		}
	}

	private final class oauth_handler implements HttpHandler {
		private static final String PREFIX = "/api/auth/oauth/";

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String path = exchange.getRequestURI().getPath();
			if (!path.startsWith(PREFIX)) {
				write_json(exchange, 404, json_util.json_object("error", "Not found"));
				return;
			}
			String remainder = path.substring(PREFIX.length());
			if ("config".equals(remainder) || remainder.startsWith("config?")) {
				if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
					write_json(exchange, 405, json_util.json_object("error", "Method not allowed"));
					return;
				}
				write_json(exchange, 200, server_configuration.get().oauth_status_json());
				return;
			}
			int slash = remainder.indexOf('/');
			if (slash < 0) {
				write_json(exchange, 404, json_util.json_object("error", "Not found"));
				return;
			}
			String provider_id = remainder.substring(0, slash);
			String action = remainder.substring(slash + 1);
			try {
				oauth_provider provider = oauth_provider.from_id(provider_id);
				if ("start".equalsIgnoreCase(action)) {
					handle_start(exchange, provider);
					return;
				}
				if ("callback".equalsIgnoreCase(action)) {
					handle_callback(exchange, provider);
					return;
				}
				write_json(exchange, 404, json_util.json_object("error", "Not found"));
			} catch (IllegalArgumentException e) {
				write_json(exchange, 400, json_util.json_object("error", e.getMessage()));
			} catch (Exception e) {
				write_html(exchange, 400, oauth_error_page(e.getMessage()));
			}
		}

		private void handle_start(HttpExchange exchange, oauth_provider provider) throws Exception {
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				write_json(exchange, 405, json_util.json_object("error", "Method not allowed"));
				return;
			}
			URI target = oauth_service.start_sign_in(provider);
			exchange.getResponseHeaders().set("Location", target.toString());
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		}

		private void handle_callback(HttpExchange exchange, oauth_provider provider) throws Exception {
			String code = query_param(exchange, "code").orElse(null);
			String state = query_param(exchange, "state").orElse(null);
			if ((code == null || state == null) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
				String body = read_body(exchange);
				if (code == null) {
					code = form_param(body, "code");
				}
				if (state == null) {
					state = form_param(body, "state");
				}
			}
			if (code == null || state == null) {
				write_html(exchange, 400, oauth_error_page("Missing OAuth authorization code"));
				return;
			}
			session_manager.session_record session = oauth_service.complete_sign_in(provider, code, state);
			String username = runtime.database().users()
					.find_by_id(session.user_id())
					.map(user_repository.user_row::username)
					.orElse("user");
			write_html(exchange, 200, oauth_success_page(session.access_token(), username));
		}

		private static String form_param(String body, String name) {
			if (body == null) {
				return null;
			}
			for (String part : body.split("&")) {
				int eq = part.indexOf('=');
				if (eq > 0 && name.equals(part.substring(0, eq))) {
					return URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
				}
			}
			return null;
		}

		private static String oauth_success_page(String access_token, String username) {
			return """
					<!DOCTYPE html><html><head><meta charset="utf-8"><title>Signed in</title></head><body>
					<p>Signing you in…</p>
					<script>
					location.replace("/?oauthToken=" + encodeURIComponent("%s") + "&oauthUser=" + encodeURIComponent("%s"));
					</script>
					</body></html>
					""".formatted(json_util.escape(access_token), json_util.escape(username));
		}

		private static String oauth_error_page(String message) {
			return """
					<!DOCTYPE html><html><head><meta charset="utf-8"><title>Sign-in failed</title></head><body>
					<h1>Sign-in failed</h1><p>%s</p><p><a href="/">Return to Server Browser</a></p>
					</body></html>
					""".formatted(json_util.escape(message == null ? "Unknown error" : message));
		}
	}

	private final class session_handler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				write_json(exchange, 405, json_util.json_object("error", "Method not allowed"));
				return;
			}
			Optional<String> token = bearer_token(exchange);
			if (token.isEmpty() || !runtime.authentication().authorize_proxy_request(token.get())) {
				write_json(exchange, 401, json_util.json_object("valid", "false"));
				return;
			}
			Optional<session_manager.session_record> session =
					runtime.authentication().sessions().validate_session(token.get());
			if (session.isEmpty()) {
				write_json(exchange, 401, json_util.json_object("valid", "false"));
				return;
			}
			write_json(
					exchange,
					200,
					json_util.json_object(
							"valid", "true",
							"userId", session.get().user_id(),
							"expiresAt", session.get().expires_at().toString()));
		}
	}

	private final class browse_handler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			if (!"POST".equalsIgnoreCase(method) && !"GET".equalsIgnoreCase(method)) {
				write_json(exchange, 405, json_util.json_object("error", "Method not allowed"));
				return;
			}
			Optional<String> token = bearer_token(exchange);
			if (token.isEmpty() || !runtime.authentication().authorize_proxy_request(token.get())) {
				write_json(exchange, 401, json_util.json_object("error", "Unauthorized"));
				return;
			}
			String url = null;
			if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				url = query_param(exchange, "url").orElse(null);
			} else {
				byte[] body_bytes = exchange.getRequestBody().readAllBytes();
				String body = new String(body_bytes, StandardCharsets.UTF_8);
				url = json_util.extract_string(body, "url");
			}
			if (url == null || url.isBlank()) {
				write_json(exchange, 400, json_util.json_object("error", "url required"));
				return;
			}
			try {
				String target_url = resolve_target_url(url);
				if (!browse_allowed_target.is_allowed(target_url)) {
					write_json(exchange, 400, json_util.json_object("error", "URL is not allowed"));
					return;
				}
				Optional<fast_page_fetcher.fetch_result> response = page_fetcher.fetch(
						URI.create(target_url),
						kind_for_url(target_url));
				if (response.isEmpty()) {
					write_json(exchange, 502, json_util.json_object("error", "Failed to fetch URL"));
					return;
				}
				log_browse_async(token.get(), target_url);
				write_browse(exchange, response.get(), token.get());
			} catch (Exception e) {
				write_json(exchange, 400, json_util.json_object("error", e.getMessage()));
			}
		}
	}

	private final class browse_resource_handler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				write_json(exchange, 405, json_util.json_object("error", "Method not allowed"));
				return;
			}
			Optional<String> token = browse_access_token(exchange);
			if (token.isEmpty() || !runtime.authentication().authorize_proxy_request(token.get())) {
				write_json(exchange, 401, json_util.json_object("error", "Unauthorized"));
				return;
			}
			String url = query_param(exchange, "url").orElse(null);
			if (url == null || url.isBlank()) {
				write_json(exchange, 400, json_util.json_object("error", "url required"));
				return;
			}
			if (!browse_allowed_target.is_allowed(url)) {
				write_json(exchange, 400, json_util.json_object("error", "URL is not allowed"));
				return;
			}
			browse_resource_kind kind = kind_for_url(url);
			Optional<fast_page_fetcher.fetch_result> cached = resource_cache.get(url);
			if (cached.isPresent()) {
				write_cached_resource(exchange, cached.get(), token.get());
				return;
			}
			Optional<fast_page_fetcher.fetch_result> response = load_resource(url, kind);
			if (response.isEmpty()) {
				write_json(exchange, 502, json_util.json_object("error", "Failed to fetch resource"));
				return;
			}
			write_resource(exchange, response.get(), token.get());
		}
	}

	private final class security_pin_handler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				write_json(exchange, 405, json_util.json_object("error", "Method not allowed"));
				return;
			}
			try {
				certificate_pin_provider.certificate_pin pin = new certificate_pin_provider().load_pin();
				write_json(
						exchange,
						200,
						json_util.json_object(
								"zone", server_browser_org_phonebook.ZONE,
								"sha256Pin", pin.sha256_pin(),
								"subject", pin.subject(),
								"issuer", pin.issuer(),
								"notAfter", String.valueOf(pin.not_after_epoch_ms()),
								"tlsProtocols", "TLSv1.3,TLSv1.2"));
			} catch (Exception e) {
				write_json(exchange, 500, json_util.json_object("error", e.getMessage()));
			}
		}
	}

	private final class dns_phonebook_handler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				write_json(exchange, 405, json_util.json_object("error", "Method not allowed"));
				return;
			}
			try {
				var records = runtime.phonebook().list_records();
				String entries = records.stream()
						.map(record -> json_util.json_object(
								"name", record.record_name(),
								"type", record.record_type(),
								"target", record.target(),
								"port", record.port() == null ? "null" : String.valueOf(record.port())))
						.collect(Collectors.joining(","));
				write_json(
						exchange,
						200,
						"{\"zone\":\"" + server_browser_org_phonebook.ZONE + "\",\"records\":[" + entries + "]}");
			} catch (Exception e) {
				write_json(exchange, 500, json_util.json_object("error", e.getMessage()));
			}
		}
	}

	private final class dns_resolve_handler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
				write_json(exchange, 405, json_util.json_object("error", "Method not allowed"));
				return;
			}
			String query = exchange.getRequestURI().getQuery();
			String host = null;
			if (query != null) {
				for (String part : query.split("&")) {
					if (part.startsWith("host=")) {
						host = java.net.URLDecoder.decode(part.substring(5), StandardCharsets.UTF_8);
					}
				}
			}
			if (host == null || host.isBlank()) {
				write_json(exchange, 400, json_util.json_object("error", "host query parameter required"));
				return;
			}
			try {
				Optional<server_browser_org_phonebook.phonebook_resolution> resolution =
						runtime.phonebook().resolve_host(host);
				if (resolution.isEmpty()) {
					write_json(exchange, 404, json_util.json_object("error", "No phonebook record"));
					return;
				}
				var resolved = resolution.get();
				write_json(
						exchange,
						200,
						json_util.json_object(
								"query", resolved.query(),
								"name", resolved.record_name(),
								"type", resolved.record_type(),
								"target", resolved.target(),
								"resolvedUrl", resolved.resolved_url().orElse("")));
			} catch (Exception e) {
				write_json(exchange, 500, json_util.json_object("error", e.getMessage()));
			}
		}
	}
}
