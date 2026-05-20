/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Provides SHA-256 certificate pins for the local HTTPS server.
 */
public final class certificate_pin_provider {
	public record certificate_pin(String sha256_pin, String subject, String issuer, long not_after_epoch_ms) {
	}

	private final Path keystore_path;
	private final char[] keystore_password;
	private final String alias;

	public certificate_pin_provider() {
		this(
				application_paths.ssl_keystore(),
				application_paths.ssl_keystore_password(),
				application_paths.ssl_alias());
	}

	public certificate_pin_provider(Path keystore_path, char[] keystore_password, String alias) {
		this.keystore_path = keystore_path;
		this.keystore_password = keystore_password;
		this.alias = alias;
	}

	public certificate_pin load_pin() throws Exception {
		KeyStore key_store = KeyStore.getInstance("JKS");
		try (FileInputStream input = new FileInputStream(keystore_path.toFile())) {
			key_store.load(input, keystore_password);
		}
		Certificate certificate = key_store.getCertificate(alias);
		if (!(certificate instanceof X509Certificate x509)) {
			throw new IllegalStateException("Expected X509 certificate in keystore");
		}
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		String pin = Base64.getEncoder().encodeToString(digest.digest(x509.getEncoded()));
		return new certificate_pin(
				pin,
				x509.getSubjectX500Principal().getName(),
				x509.getIssuerX500Principal().getName(),
				x509.getNotAfter().getTime());
	}

	public static boolean pins_match(String expected_pin, String observed_pin) {
		return expected_pin != null
				&& observed_pin != null
				&& MessageDigest.isEqual(expected_pin.getBytes(), observed_pin.getBytes());
	}
}
