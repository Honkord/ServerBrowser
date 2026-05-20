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
import java.nio.charset.StandardCharsets;

/**
 * Wraps handlers with TLS downgrade checks and transport-security response headers.
 */
public final class connection_security_filter implements HttpHandler {
	private final HttpHandler delegate;
	private final secure_connection_pipeline pipeline;

	public connection_security_filter(HttpHandler delegate, secure_connection_pipeline pipeline) {
		this.delegate = delegate;
		this.pipeline = pipeline;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		if (pipeline.reject_insecure_request(exchange)) {
			pipeline.apply_security_headers(exchange);
			byte[] payload = "HTTPS required".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
			exchange.sendResponseHeaders(426, payload.length);
			exchange.getResponseBody().write(payload);
			exchange.close();
			return;
		}
		pipeline.apply_security_headers(exchange);
		delegate.handle(exchange);
	}
}
