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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves the Vue web application from {@code frontend/dist/} (production build).
 */
public final class static_file_handler implements HttpHandler {
	private static final String DEFAULT_INDEX = "index.html";
	private static volatile Path frontend_root = locate_frontend_root();

	public static Path frontend_root() {
		return frontend_root;
	}

	public static Path locate_frontend_root() {
		Path dist = application_paths.frontend_dist();
		if (Files.isRegularFile(dist.resolve(DEFAULT_INDEX))) {
			text_printer.print(text_printer.format.LOG, "Serving UI from " + dist);
			return dist;
		}
		text_printer.print(
				text_printer.format.ERROR,
				"UI not built at " + dist + " — run: ./gradlew buildFrontend or mvn package");
		return dist;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
				&& !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
			write_error(exchange, 405, "Method Not Allowed");
			return;
		}
		String request_path = exchange.getRequestURI().getPath();
		if (request_path == null || request_path.isBlank() || request_path.equals("/")) {
			request_path = "/" + DEFAULT_INDEX;
		}
		Path file = frontend_root.resolve(request_path.replaceFirst("^/", "")).normalize();
		if (!file.startsWith(frontend_root)) {
			write_error(exchange, 403, "Forbidden");
			return;
		}
		if (!Files.exists(file) || Files.isDirectory(file)) {
			if (is_spa_route(request_path)) {
				file = frontend_root.resolve(DEFAULT_INDEX);
			} else {
				write_error(exchange, 404, "Not found");
				return;
			}
		}
		if (!Files.isRegularFile(file)) {
			write_error(
					exchange,
					404,
					"UI not found — build the frontend: cd frontend && npm install && npm run build");
			return;
		}
		byte[] payload = Files.readAllBytes(file);
		exchange.getResponseHeaders().set("Content-Type", content_type(file));
		if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
			exchange.sendResponseHeaders(200, -1);
		} else {
			exchange.sendResponseHeaders(200, payload.length);
			try (OutputStream output = exchange.getResponseBody()) {
				output.write(payload);
			}
		}
	}

	private static boolean is_spa_route(String request_path) {
		String name = request_path.substring(request_path.lastIndexOf('/') + 1);
		return !name.contains(".");
	}

	private static String content_type(Path file) {
		String name = file.getFileName().toString().toLowerCase();
		if (name.endsWith(".html")) {
			return "text/html; charset=UTF-8";
		}
		if (name.endsWith(".css")) {
			return "text/css; charset=UTF-8";
		}
		if (name.endsWith(".js")) {
			return "application/javascript; charset=UTF-8";
		}
		if (name.endsWith(".svg")) {
			return "image/svg+xml";
		}
		if (name.endsWith(".png")) {
			return "image/png";
		}
		if (name.endsWith(".ico")) {
			return "image/x-icon";
		}
		if (name.endsWith(".woff2")) {
			return "font/woff2";
		}
		if (name.endsWith(".woff")) {
			return "font/woff";
		}
		if (name.endsWith(".map")) {
			return "application/json";
		}
		return "application/octet-stream";
	}

	private static void write_error(HttpExchange exchange, int status, String message) throws IOException {
		byte[] payload = message.getBytes();
		exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
		exchange.sendResponseHeaders(status, payload.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(payload);
		}
	}
}
