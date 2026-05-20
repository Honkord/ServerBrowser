/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsParameters;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

/**
 * Hardens TLS and applies transport-security headers on the client↔server pipeline.
 */
public final class secure_connection_pipeline {
	private static final String[] SECURE_PROTOCOLS = { "TLSv1.3", "TLSv1.2" };
	private static final Set<String> BLOCKED_CIPHER_SUITES = Set.of(
			"TLS_RSA_WITH_AES_128_CBC_SHA",
			"TLS_RSA_WITH_AES_256_CBC_SHA",
			"TLS_RSA_EXPORT_WITH_RC4_40_MD5",
			"SSL_RSA_WITH_RC4_128_MD5");

	public void configure_tls(HttpsParameters params, SSLContext ssl_context) {
		SSLEngine engine = ssl_context.createSSLEngine();
		params.setNeedClientAuth(false);
		params.setProtocols(SECURE_PROTOCOLS);
		params.setCipherSuites(filter_cipher_suites(engine.getEnabledCipherSuites()));
		SSLParameters ssl_parameters = ssl_context.getDefaultSSLParameters();
		ssl_parameters.setProtocols(SECURE_PROTOCOLS);
		ssl_parameters.setEndpointIdentificationAlgorithm("HTTPS");
		ssl_parameters.setUseCipherSuitesOrder(true);
		params.setSSLParameters(ssl_parameters);
	}

	public void apply_security_headers(HttpExchange exchange) {
		var headers = exchange.getResponseHeaders();
		headers.set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
		headers.set("X-Content-Type-Options", "nosniff");
		headers.set("X-Frame-Options", "DENY");
		headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
		headers.set(
				"Content-Security-Policy",
				"default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
						+ "img-src 'self' data: https: blob:; connect-src 'self'; "
						+ "frame-src 'self' blob: data:; object-src 'none'; base-uri 'self'; form-action 'self'");
		headers.set("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
		headers.set("X-Packet-Security", "TLS-AES-GCM; integrity-sealed");
		headers.set("Cache-Control", "no-store");
	}

	public boolean reject_insecure_request(HttpExchange exchange) {
		String scheme = exchange.getRequestURI().getScheme();
		if (scheme != null && scheme.equalsIgnoreCase("http")) {
			return true;
		}
		String forwarded_proto = exchange.getRequestHeaders().getFirst("X-Forwarded-Proto");
		return forwarded_proto != null && forwarded_proto.equalsIgnoreCase("http");
	}

	private static String[] filter_cipher_suites(String[] enabled) {
		Set<String> allowed = new LinkedHashSet<>();
		for (String suite : enabled) {
			if (!BLOCKED_CIPHER_SUITES.contains(suite)) {
				allowed.add(suite);
			}
		}
		return allowed.toArray(new String[0]);
	}
}
