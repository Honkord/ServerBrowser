/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing, salt generation, and verification.
 */
public final class password_security {
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 120_000;
	private static final int KEY_LENGTH_BITS = 256;
	private static final int SALT_BYTES = 16;

	public byte[] generate_salt() {
		byte[] salt = new byte[SALT_BYTES];
		RANDOM.nextBytes(salt);
		return salt;
	}

	public String hash_password(char[] plain_password, byte[] salt) {
		byte[] hash = derive_key(plain_password, salt);
		return Base64.getEncoder().encodeToString(hash);
	}

	public boolean verify_password(char[] plain_password, String stored_hash, byte[] salt) {
		if (plain_password == null || stored_hash == null || salt == null) {
			return false;
		}
		String candidate = hash_password(plain_password, salt);
		return constant_time_equals(candidate, stored_hash);
	}

	private static byte[] derive_key(char[] plain_password, byte[] salt) {
		PBEKeySpec spec = new PBEKeySpec(plain_password, salt, ITERATIONS, KEY_LENGTH_BITS);
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
			return factory.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new IllegalStateException("Password hashing unavailable", e);
		} finally {
			spec.clearPassword();
		}
	}

	private static boolean constant_time_equals(String left, String right) {
		return Arrays.equals(left.getBytes(), right.getBytes());
	}
}
