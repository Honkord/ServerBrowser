/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Redirects {@code *.server_browser.org} hostnames (local hosts file) into the web app.
 */
public final class phonebook_host_redirect_handler implements HttpHandler {
	private final server_browser_org_phonebook phonebook;
	private final HttpHandler fallback;
	private final int https_port;

	public phonebook_host_redirect_handler(
			server_browser_org_phonebook phonebook,
			HttpHandler fallback,
			int https_port) {
		this.phonebook = phonebook;
		this.fallback = fallback;
		this.https_port = https_port;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String host_header = exchange.getRequestHeaders().getFirst("Host");
		if (host_header == null) {
			fallback.handle(exchange);
			return;
		}
		String hostname = host_header.split(":")[0].toLowerCase();
		if (!is_phonebook_host(hostname)) {
			fallback.handle(exchange);
			return;
		}
		try {
			Optional<server_browser_org_phonebook.phonebook_resolution> resolution =
					phonebook.resolve_host(hostname);
			if (resolution.isEmpty() || resolution.get().resolved_url().isEmpty()) {
				write_help(exchange, hostname);
				return;
			}
			String target = resolution.get().resolved_url().get();
			String redirect = "https://127.0.0.1:" + https_port + "/?url="
					+ URLEncoder.encode(target, StandardCharsets.UTF_8)
					+ "&phonebook="
					+ URLEncoder.encode(hostname, StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Location", redirect);
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		} catch (Exception e) {
			write_help(exchange, hostname);
		}
	}

	private static boolean is_phonebook_host(String hostname) {
		return hostname.equals(server_browser_org_phonebook.ZONE)
				|| hostname.endsWith("." + server_browser_org_phonebook.ZONE);
	}

	private static void write_help(HttpExchange exchange, String hostname) throws IOException {
		String body = """
				<html><body style="font-family:sans-serif;padding:2rem">
				<h1>Server Browser phonebook</h1>
				<p><strong>%s</strong> is a local name only.</p>
				<ol>
				<li>Run <code>scripts/install_phonebook_hosts.sh</code> (or add hosts on Windows).</li>
				<li>Open the app at <a href="https://127.0.0.1:8443/">https://localhost:8443</a></li>
				<li>Sign in, then search for <code>%s</code> in the app search box.</li>
				</ol>
				</body></html>
				""".formatted(hostname, hostname);
		byte[] payload = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
		exchange.sendResponseHeaders(200, payload.length);
		exchange.getResponseBody().write(payload);
		exchange.close();
	}
}
