/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Generates user identifiers, passwords, API keys, and session tokens.
 */
public final class credential_generator {
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int SECURE_PASSWORD_LENGTH = 24;
	private static final int API_KEY_BYTES = 32;
	private static final int ACCESS_TOKEN_BYTES = 32;
	private static final int REFRESH_TOKEN_BYTES = 48;

	private static final String PASSWORD_ALPHABET =
			"ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*";

	public String create_user_id() {
		return UUID.randomUUID().toString();
	}

	public String generate_secure_password() {
		StringBuilder password = new StringBuilder(SECURE_PASSWORD_LENGTH);
		for (int i = 0; i < SECURE_PASSWORD_LENGTH; i++) {
			int index = RANDOM.nextInt(PASSWORD_ALPHABET.length());
			password.append(PASSWORD_ALPHABET.charAt(index));
		}
		return password.toString();
	}

	public String generate_api_key() {
		return "sb_api_" + random_token(API_KEY_BYTES);
	}

	public String generate_access_token() {
		return random_token(ACCESS_TOKEN_BYTES);
	}

	public String generate_refresh_token() {
		return random_token(REFRESH_TOKEN_BYTES);
	}

	private static String random_token(int byte_length) {
		byte[] bytes = new byte[byte_length];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
