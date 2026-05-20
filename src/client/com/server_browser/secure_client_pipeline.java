/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * HTTPS client pipeline with certificate pinning for the local gateway.
 */
public final class secure_client_pipeline {
	private final HttpClient http_client;
	private final String expected_pin;

	public secure_client_pipeline(String expected_pin) throws NoSuchAlgorithmException, KeyManagementException {
		this.expected_pin = expected_pin;
		this.http_client = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_2)
				.connectTimeout(Duration.ofSeconds(15))
				.sslContext(create_pinned_context(expected_pin))
				.build();
	}

	public HttpResponse<byte[]> get(URI uri, String access_token) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", "Bearer " + access_token)
				.GET()
				.build();
		return http_client.send(request, HttpResponse.BodyHandlers.ofByteArray());
	}

	private static SSLContext create_pinned_context(String expected_pin)
			throws NoSuchAlgorithmException, KeyManagementException {
		TrustManager[] trust_managers = new TrustManager[] {
			new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
					if (chain == null || chain.length == 0) {
						throw new java.security.cert.CertificateException("No server certificate");
					}
					String observed = sha256_pin(chain[0]);
					if (!certificate_pin_provider.pins_match(expected_pin, observed)) {
						throw new java.security.cert.CertificateException("Certificate pin mismatch");
					}
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
			}
		};
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, trust_managers, null);
		return context;
	}

	private static String sha256_pin(X509Certificate certificate) throws java.security.cert.CertificateException {
		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			return java.util.Base64.getEncoder().encodeToString(digest.digest(certificate.getEncoded()));
		} catch (Exception e) {
			throw new java.security.cert.CertificateException("Unable to compute pin", e);
		}
	}
}
