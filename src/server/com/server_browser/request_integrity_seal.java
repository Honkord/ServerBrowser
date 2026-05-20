/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC seals API requests so tampered packets are rejected after TLS decryption.
 */
public final class request_integrity_seal {
	public static final String HEADER_TIMESTAMP = "X-Request-Timestamp";
	public static final String HEADER_SEAL = "X-Request-Seal";

	private final Duration max_skew;

	public request_integrity_seal() {
		this(Duration.ofMinutes(5));
	}

	public request_integrity_seal(Duration max_skew) {
		this.max_skew = max_skew;
	}

	public String seal(String secret, String timestamp, String method, String path, byte[] body)
			throws NoSuchAlgorithmException, InvalidKeyException {
		String payload = timestamp + "\n" + method.toUpperCase() + "\n" + path + "\n"
				+ Base64.getEncoder().encodeToString(body == null ? new byte[0] : body);
		return compute_hmac(secret, payload);
	}

	public boolean verify(
			String secret,
			String timestamp,
			String method,
			String path,
			byte[] body,
			String provided_seal) {
		if (secret == null || timestamp == null || provided_seal == null) {
			return false;
		}
		try {
			Instant request_time = Instant.parse(timestamp);
			Instant now = Instant.now();
			if (request_time.isBefore(now.minus(max_skew)) || request_time.isAfter(now.plus(max_skew))) {
				return false;
			}
			String expected = seal(secret, timestamp, method, path, body);
			return MessageDigestConstantTime.equals(expected, provided_seal);
		} catch (Exception e) {
			return false;
		}
	}

	private static String compute_hmac(String secret, String payload)
			throws NoSuchAlgorithmException, InvalidKeyException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
	}

	private static final class MessageDigestConstantTime {
		static boolean equals(String left, String right) {
			if (left == null || right == null) {
				return false;
			}
			byte[] a = left.getBytes(StandardCharsets.UTF_8);
			byte[] b = right.getBytes(StandardCharsets.UTF_8);
			if (a.length != b.length) {
				return false;
			}
			int result = 0;
			for (int i = 0; i < a.length; i++) {
				result |= a[i] ^ b[i];
			}
			return result == 0;
		}
	}
}
