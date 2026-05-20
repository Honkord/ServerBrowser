/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 * Validates TLS peer certificates and hostname alignment.
 */
public final class tls_certificate_validation {
	public boolean validate(SSLSocket socket, String expected_hostname) {
		if (socket == null || expected_hostname == null || expected_hostname.isBlank()) {
			return false;
		}
		try {
			SSLSession session = socket.getSession();
			if (session == null) {
				return false;
			}
			Certificate[] chain = session.getPeerCertificates();
			if (chain == null || chain.length == 0) {
				return false;
			}
			if (!(chain[0] instanceof X509Certificate certificate)) {
				return false;
			}
			certificate.checkValidity();
			String peer_host = session.getPeerHost();
			return peer_host != null && matches_hostname(expected_hostname, peer_host);
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean matches_hostname(String expected, String peer_host) {
		String normalized_expected = expected.trim().toLowerCase();
		String normalized_peer = peer_host.trim().toLowerCase();
		return normalized_expected.equals(normalized_peer)
				|| normalized_peer.endsWith("." + normalized_expected);
	}
}
