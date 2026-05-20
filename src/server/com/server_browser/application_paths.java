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
 * Resolves project root, {@code data/} layout, and delegates settings to {@link server_configuration}.
 */
public final class application_paths {
	public static final String ROOT_PROPERTY = "server.browser.root";
	public static final String DATA_DIR_PROPERTY = "server.browser.data.dir";

	private static final Path PROJECT_ROOT = locate_project_root();
	private static final Path DATA_ROOT = resolve_data_root();

	static {
		try {
			server_configuration.initialize(DATA_ROOT);
		} catch (IOException error) {
			throw new ExceptionInInitializerError(error);
		}
	}

	private application_paths() {
	}

	public static Path project_root() {
		return PROJECT_ROOT;
	}

	public static Path data_root() {
		return DATA_ROOT;
	}

	public static Path config_file() {
		return server_configuration.get().config_dir().resolve("application.properties");
	}

	public static int server_port() {
		return Integer.parseInt(get("server.port", "8443"));
	}

	public static Path database_file() {
		return DATA_ROOT.resolve(get("db.file", "db/server_browser.db"));
	}

	public static Path database_schema() {
		return DATA_ROOT.resolve(get("db.schema", "db/schema.sql"));
	}

	public static Path ssl_keystore() {
		return DATA_ROOT.resolve(get("ssl.keystore", "ssl/key.jks"));
	}

	public static char[] ssl_keystore_password() {
		return get("ssl.keystore.password", "password").toCharArray();
	}

	public static String ssl_alias() {
		return get("ssl.alias", "serverbrowser");
	}

	public static Path dns_phonebook_seed() {
		return DATA_ROOT.resolve(get("dns.phonebook.seed", "dns_phonebook/server_browser.org.json"));
	}

	public static Path frontend_dist() {
		return PROJECT_ROOT.resolve(get("frontend.dist", "frontend/dist"));
	}

	public static String get(String key, String default_value) {
		return server_configuration.get(key, default_value);
	}

	private static Path resolve_data_root() {
		String override = System.getProperty(DATA_DIR_PROPERTY);
		if (override != null && !override.isBlank()) {
			Path path = Path.of(override).toAbsolutePath().normalize();
			ensure_data_layout(path);
			return path;
		}
		String configured = System.getProperty("data.dir");
		if (configured == null || configured.isBlank()) {
			configured = bootstrap_properties().getProperty("data.dir", "data");
		}
		if (configured == null || configured.isBlank()) {
			configured = "data";
		}
		Path path = PROJECT_ROOT.resolve(configured.trim()).normalize();
		ensure_data_layout(path);
		return path;
	}

	private static Properties bootstrap_properties() {
		Properties properties = new Properties();
		Path bootstrap = PROJECT_ROOT.resolve("data").resolve("config").resolve("application.properties");
		if (Files.isRegularFile(bootstrap)) {
			try (InputStream input = Files.newInputStream(bootstrap)) {
				properties.load(input);
			} catch (IOException error) {
				text_printer.print(text_printer.format.ERROR, "Failed to load bootstrap config: " + error.getMessage());
			}
		}
		return properties;
	}

	private static void ensure_data_layout(Path data_root) {
		try {
			Files.createDirectories(data_root.resolve("config"));
			Files.createDirectories(data_root.resolve("db"));
			Files.createDirectories(data_root.resolve("ssl"));
			Files.createDirectories(data_root.resolve("dns_phonebook"));
		} catch (IOException error) {
			throw new IllegalStateException("Failed to create data directories under " + data_root, error);
		}
	}

	private static Path locate_project_root() {
		String property = System.getProperty(ROOT_PROPERTY);
		if (property != null && !property.isBlank()) {
			return Path.of(property).toAbsolutePath().normalize();
		}
		Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
		Path[] markers = {
				Path.of("settings.gradle.kts"),
				Path.of("pom.xml"),
				Path.of("data", "config", "application.properties"),
				Path.of("build.gradle.kts")
		};
		Path current = cwd;
		for (int depth = 0; depth < 6; depth++) {
			for (Path marker : markers) {
				if (Files.exists(current.resolve(marker))) {
					return current;
				}
			}
			Path parent = current.getParent();
			if (parent == null) {
				break;
			}
			current = parent;
		}
		return cwd;
	}
}
