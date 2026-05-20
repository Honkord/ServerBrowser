/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;

/**
 * Server runtime with SQLite database, authentication, proxy, and frontend gateway.
 */
public final class server_runtime extends server {
	private final sql_database database;
	private final authentication_subsystem authentication;
	private final response_streamer response_streamer;
	private final server_browser_org_phonebook phonebook;
	private final secure_connection_pipeline secure_pipeline;
	private final request_integrity_seal integrity_seal;

	public server_runtime(sql_database database) {
		this.database = database;
		this.authentication = new authentication_subsystem(database);
		this.authentication.restore_roles_from_database();
		this.response_streamer = new response_streamer(database);
		this.phonebook = new server_browser_org_phonebook(database.dns_phonebook());
		this.secure_pipeline = new secure_connection_pipeline();
		this.integrity_seal = new request_integrity_seal();
	}

	public static server_runtime bootstrap() throws Exception {
		sql_database database = sql_database.open();
		database.purge_expired();
		text_printer.print(
				text_printer.format.LOG,
				"Database ready at " + application_paths.database_file());
		return new server_runtime(database);
	}

	public sql_database database() {
		return database;
	}

	public authentication_subsystem authentication() {
		return authentication;
	}

	public response_streamer response_streamer() {
		return response_streamer;
	}

	public server_browser_org_phonebook phonebook() {
		return phonebook;
	}

	public secure_connection_pipeline secure_pipeline() {
		return secure_pipeline;
	}

	public request_integrity_seal integrity_seal() {
		return integrity_seal;
	}

	@Override
	public String retrieve_response() {
		return "Server Browser";
	}

	@Override
	public void host_server() {
		int port = application_paths.server_port();
		https_credential_generator credentials = new https_credential_generator();

		try {
			credentials.ensure_keystore();
			SSLContext ssl_context = credentials.create_ssl_context();
			HttpsServer https_server = HttpsServer.create(new InetSocketAddress(port), 0);
			secure_connection_pipeline pipeline = secure_pipeline;
			https_server.setHttpsConfigurator(new HttpsConfigurator(ssl_context) {
				@Override
				public void configure(HttpsParameters params) {
					pipeline.configure_tls(params, getSSLContext());
				}
			});

			frontend_api_gateway gateway = new frontend_api_gateway(this);
			gateway.register(https_server);

			HttpHandler static_files = new connection_security_filter(new static_file_handler(), pipeline);
			HttpHandler web_app = new connection_security_filter(
					new phonebook_host_redirect_handler(phonebook, static_files, port),
					pipeline);
			https_server.createContext("/assets", web_app);
			https_server.createContext("/index.html", web_app);
			https_server.createContext(
					"/proxy",
					new connection_security_filter(new proxy_handler(this), pipeline));
			https_server.createContext("/", web_app);

			https_server.setExecutor(null);
			https_server.start();

			text_printer.print(
					text_printer.format.LOG,
					"HTTPS server started at https://localhost:" + port);
			text_printer.print(
					text_printer.format.LOG,
					"Open https://localhost:" + port + "/ in your browser");
		} catch (Exception e) {
			text_printer.print(
					text_printer.format.ERROR,
					"Error starting HTTPS server: " + e.getMessage());
			e.printStackTrace();
		}
	}

	static final class proxy_handler implements HttpHandler {
		private final server_runtime runtime;

		proxy_handler(server_runtime runtime) {
			this.runtime = runtime;
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String access_token = exchange.getRequestHeaders().getFirst("Authorization");
			if (access_token != null && access_token.startsWith("Bearer ")) {
				access_token = access_token.substring("Bearer ".length());
			}
			if (access_token == null || !runtime.authentication().authorize_proxy_request(access_token)) {
				write_text(exchange, 401, "Unauthorized");
				return;
			}

			String target = exchange.getRequestHeaders().getFirst("X-Target-Url");
			if (target == null || target.isBlank()) {
				write_text(exchange, 400, "Missing X-Target-Url header");
				return;
			}

			String user_id = runtime.authentication().sessions()
					.validate_session(access_token)
					.map(session_manager.session_record::user_id)
					.orElse(null);

			exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
			exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
			exchange.sendResponseHeaders(200, 0);
			response_streamer.stream_result result = runtime.response_streamer().stream_proxy_response(
					exchange.getResponseBody(),
					URI.create(target),
					exchange.getRequestMethod(),
					flatten_headers(exchange.getRequestHeaders()),
					exchange.getRequestBody().readAllBytes(),
					user_id);
			if (!result.success()) {
				text_printer.print(text_printer.format.ERROR, "Proxy stream failed for " + target);
			}
			exchange.close();
		}
	}

	private static Map<String, String> flatten_headers(Map<String, List<String>> headers) {
		Map<String, String> flattened = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				flattened.put(entry.getKey(), entry.getValue().get(0));
			}
		}
		return flattened;
	}

	private static void write_text(HttpExchange exchange, int status, String body) throws IOException {
		byte[] payload = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
		exchange.sendResponseHeaders(status, payload.length);
		try (OutputStream output = exchange.getResponseBody()) {
			output.write(payload);
		}
	}

	public static void main(String[] args) throws Exception {
		server_runtime runtime = server_runtime.bootstrap();
		runtime.host_server();
	}
}
