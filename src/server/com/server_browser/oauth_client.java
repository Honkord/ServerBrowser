/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 authorization-code + PKCE client for Google, Microsoft, and Apple.
 */
public final class oauth_client {
	public record oauth_user(String subject, String email, String display_name) {
	}

	public record pkce_pair(String state, String code_verifier, String code_challenge) {
	}

	private static final Duration TIMEOUT = Duration.ofSeconds(15);
	private static final SecureRandom RANDOM = new SecureRandom();

	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(TIMEOUT)
			.version(HttpClient.Version.HTTP_2)
			.build();

	public pkce_pair create_pkce() {
		String verifier = random_url_safe(48);
		String challenge = base64_url(sha256(verifier.getBytes(StandardCharsets.US_ASCII)));
		String state = random_url_safe(24);
		return new pkce_pair(state, verifier, challenge);
	}

	public URI authorization_url(oauth_provider provider, pkce_pair pkce) {
		oauth_config.provider_settings settings = oauth_config.for_provider(provider);
		if (!settings.enabled()) {
			throw new IllegalStateException(provider.id() + " OAuth is not configured");
		}
		Map<String, String> params = new LinkedHashMap<>();
		params.put("client_id", settings.client_id());
		params.put("response_type", "code");
		params.put("redirect_uri", settings.redirect_uri());
		params.put("scope", scopes(provider));
		params.put("state", pkce.state());
		params.put("code_challenge", pkce.code_challenge());
		params.put("code_challenge_method", "S256");
		if (provider == oauth_provider.MICROSOFT) {
			params.put("response_mode", "query");
		}
		if (provider == oauth_provider.APPLE) {
			params.put("response_mode", "form_post");
		}
		return URI.create(authorize_endpoint(provider) + "?" + encode(params));
	}

	public oauth_user exchange_code(oauth_provider provider, String code, String code_verifier) throws Exception {
		oauth_config.provider_settings settings = oauth_config.for_provider(provider);
		Map<String, String> form = new LinkedHashMap<>();
		form.put("grant_type", "authorization_code");
		form.put("code", code);
		form.put("redirect_uri", settings.redirect_uri());
		form.put("client_id", settings.client_id());
		form.put("code_verifier", code_verifier);
		if (settings.client_secret() != null && !settings.client_secret().isBlank()) {
			form.put("client_secret", settings.client_secret());
		}
		HttpRequest request = HttpRequest.newBuilder(URI.create(token_endpoint(provider)))
				.timeout(TIMEOUT)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(encode(form)))
				.build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IllegalStateException("Token exchange failed: " + response.body());
		}
		String access_token = json_util.extract_string(response.body(), "access_token");
		String id_token = json_util.extract_string(response.body(), "id_token");
		if (access_token == null && id_token != null) {
			return user_from_id_token(id_token);
		}
		if (access_token == null) {
			throw new IllegalStateException("No access token returned");
		}
		return fetch_user(provider, access_token, id_token);
	}

	private oauth_user fetch_user(oauth_provider provider, String access_token, String id_token) throws Exception {
		return switch (provider) {
			case GOOGLE -> fetch_google_user(access_token);
			case MICROSOFT -> fetch_microsoft_user(access_token);
			case APPLE -> user_from_id_token(id_token == null ? access_token : id_token);
		};
	}

	private oauth_user fetch_google_user(String access_token) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("https://openidconnect.googleapis.com/v1/userinfo"))
				.timeout(TIMEOUT)
				.header("Authorization", "Bearer " + access_token)
				.GET()
				.build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		String body = response.body();
		return new oauth_user(
				require_field(body, "sub"),
				json_util.extract_string(body, "email"),
				first_present(body, "name", "given_name"));
	}

	private oauth_user fetch_microsoft_user(String access_token) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("https://graph.microsoft.com/v1.0/me"))
				.timeout(TIMEOUT)
				.header("Authorization", "Bearer " + access_token)
				.GET()
				.build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		String body = response.body();
		return new oauth_user(
				require_field(body, "id"),
				first_present(body, "mail", "userPrincipalName"),
				first_present(body, "displayName"));
	}

	private oauth_user user_from_id_token(String id_token) {
		String[] parts = id_token.split("\\.");
		if (parts.length < 2) {
			throw new IllegalStateException("Invalid id_token");
		}
		String payload = new String(Base64.getUrlDecoder().decode(pad(parts[1])), StandardCharsets.UTF_8);
		return new oauth_user(
				require_field(payload, "sub"),
				json_util.extract_string(payload, "email"),
				first_present(payload, "name", "email"));
	}

	private static String authorize_endpoint(oauth_provider provider) {
		return switch (provider) {
			case GOOGLE -> "https://accounts.google.com/o/oauth2/v2/auth";
			case MICROSOFT -> "https://login.microsoftonline.com/"
					+ oauth_config.microsoft().tenant()
					+ "/oauth2/v2.0/authorize";
			case APPLE -> "https://appleid.apple.com/auth/authorize";
		};
	}

	private static String token_endpoint(oauth_provider provider) {
		return switch (provider) {
			case GOOGLE -> "https://oauth2.googleapis.com/token";
			case MICROSOFT -> "https://login.microsoftonline.com/"
					+ oauth_config.microsoft().tenant()
					+ "/oauth2/v2.0/token";
			case APPLE -> "https://appleid.apple.com/auth/token";
		};
	}

	private static String scopes(oauth_provider provider) {
		return switch (provider) {
			case GOOGLE -> "openid email profile";
			case MICROSOFT -> "openid profile email User.Read";
			case APPLE -> "name email";
		};
	}

	private static String require_field(String json, String field) {
		String value = json_util.extract_string(json, field);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Missing OAuth field: " + field);
		}
		return value;
	}

	private static String first_present(String json, String... fields) {
		for (String field : fields) {
			String value = json_util.extract_string(json, field);
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static String encode(Map<String, String> params) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (builder.length() > 0) {
				builder.append('&');
			}
			builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
			builder.append('=');
			builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
		}
		return builder.toString();
	}

	private static String random_url_safe(int bytes) {
		byte[] buffer = new byte[bytes];
		RANDOM.nextBytes(buffer);
		return base64_url(buffer);
	}

	private static String base64_url(byte[] data) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
	}

	private static byte[] sha256(byte[] data) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(data);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static String pad(String value) {
		int padding = (4 - (value.length() % 4)) % 4;
		return value + "=".repeat(padding);
	}
}
