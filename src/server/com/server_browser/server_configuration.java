/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads runtime settings from {@code data/config/} (application + OAuth property files).
 */
public final class server_configuration {
	private static final String APP_FILE = "application.properties";
	private static final String OAUTH_FILE = "oauth.properties";
	private static final String OAUTH_EXAMPLE = "oauth.properties.example";

	private static volatile server_configuration instance;

	private final Path data_root;
	private final Path config_dir;
	private final Properties application;
	private final Properties oauth;

	private server_configuration(Path data_root, Properties application, Properties oauth) {
		this.data_root = data_root;
		this.config_dir = data_root.resolve("config");
		this.application = application;
		this.oauth = oauth;
	}

	public static void initialize(Path data_root) throws IOException {
		Path config_dir = data_root.resolve("config");
		Files.createDirectories(config_dir);
		ensure_oauth_template(config_dir);
		Properties application = load_properties(config_dir.resolve(APP_FILE));
		Properties oauth = load_properties(config_dir.resolve(OAUTH_FILE));
		instance = new server_configuration(data_root, application, oauth);
		log_startup_summary();
	}

	public static server_configuration get() {
		if (instance == null) {
			throw new IllegalStateException("server_configuration is not initialized");
		}
		return instance;
	}

	public static String get(String key, String default_value) {
		return get().lookup(key, default_value);
	}

	public static String oauth(String key, String default_value) {
		return get().lookup_oauth(key, default_value);
	}

	public Path data_root() {
		return data_root;
	}

	public Path config_dir() {
		return config_dir;
	}

	public Path oauth_config_file() {
		return config_dir.resolve(OAUTH_FILE);
	}

	public boolean oauth_provider_enabled(oauth_provider provider) {
		String prefix = "oauth." + provider.id() + ".";
		String client_id = lookup_oauth(prefix + "client_id", lookup(prefix + "client_id", ""));
		String redirect = oauth_redirect_uri(provider);
		return client_id != null && !client_id.isBlank() && redirect != null && !redirect.isBlank();
	}

	public String oauth_redirect_uri(oauth_provider provider) {
		String prefix = "oauth." + provider.id() + ".";
		String configured = lookup_oauth(prefix + "redirect_uri", lookup(prefix + "redirect_uri", ""));
		if (configured != null && !configured.isBlank()) {
			return configured.trim();
		}
		int port = Integer.parseInt(lookup("server.port", "8443"));
		return "https://localhost:" + port + "/api/auth/oauth/" + provider.id() + "/callback";
	}

	public String oauth_status_json() {
		StringBuilder json = new StringBuilder();
		json.append('{');
		json.append("\"configDir\":\"").append(json_util.escape(config_dir.toString())).append('\"');
		json.append(",\"oauthFile\":\"").append(json_util.escape(oauth_config_file().toString())).append('\"');
		json.append(",\"setupHint\":\"").append(json_util.escape(
				"Edit " + oauth_config_file() + " with client IDs and secrets from your identity provider."))
				.append('\"');
		json.append(",\"providers\":{");
		boolean first = true;
		for (oauth_provider provider : oauth_provider.values()) {
			if (!first) {
				json.append(',');
			}
			first = false;
			json.append('\"').append(provider.id()).append("\":{");
			json.append("\"enabled\":").append(oauth_provider_enabled(provider));
			json.append(",\"label\":\"").append(json_util.escape(provider_label(provider))).append('\"');
			json.append(",\"redirectUri\":\"").append(json_util.escape(oauth_redirect_uri(provider))).append('\"');
			json.append('}');
		}
		json.append("}}");
		return json.toString();
	}

	private String lookup(String key, String default_value) {
		String from_oauth = oauth.getProperty(key);
		if (from_oauth != null && !from_oauth.isBlank()) {
			return from_oauth.trim();
		}
		String from_app = application.getProperty(key);
		if (from_app != null && !from_app.isBlank()) {
			return from_app.trim();
		}
		return default_value;
	}

	private String lookup_oauth(String key, String default_value) {
		String value = oauth.getProperty(key);
		if (value != null && !value.isBlank()) {
			return value.trim();
		}
		return default_value;
	}

	private static String provider_label(oauth_provider provider) {
		return switch (provider) {
			case GOOGLE -> "Google";
			case MICROSOFT -> "Microsoft";
			case APPLE -> "Apple";
		};
	}

	private static Properties load_properties(Path file) throws IOException {
		Properties properties = new Properties();
		if (Files.isRegularFile(file)) {
			try (InputStream input = Files.newInputStream(file)) {
				properties.load(input);
			}
		}
		return properties;
	}

	private static void ensure_oauth_template(Path config_dir) throws IOException {
		Path oauth = config_dir.resolve(OAUTH_FILE);
		Path example = config_dir.resolve(OAUTH_EXAMPLE);
		if (!Files.isRegularFile(oauth) && Files.isRegularFile(example)) {
			Files.copy(example, oauth);
			text_printer.print(
					text_printer.format.LOG,
					"Created " + oauth + " from template — add OAuth client credentials.");
		}
	}

	private static void log_startup_summary() {
		server_configuration config = get();
		text_printer.print(text_printer.format.LOG, "Configuration directory: " + config.config_dir());
		for (oauth_provider provider : oauth_provider.values()) {
			String state = config.oauth_provider_enabled(provider) ? "enabled" : "disabled (not configured)";
			text_printer.print(text_printer.format.LOG, "OAuth " + provider.id() + ": " + state);
		}
	}
}
