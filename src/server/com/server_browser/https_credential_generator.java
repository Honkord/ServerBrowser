/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Generates and loads the local HTTPS keystore used by the server listener.
 */
public final class https_credential_generator {
	public static final String DEFAULT_KEYSTORE_FILE = "key.jks";
	public static final char[] DEFAULT_KEYSTORE_PASSWORD = "password".toCharArray();
	public static final String DEFAULT_ALIAS = "serverbrowser";

	private final Path keystore_path;
	private final char[] keystore_password;
	private final String alias;

	public https_credential_generator() {
		this(
				application_paths.ssl_keystore(),
				application_paths.ssl_keystore_password(),
				application_paths.ssl_alias());
	}

	public https_credential_generator(Path keystore_path, char[] keystore_password, String alias) {
		this.keystore_path = keystore_path;
		this.keystore_password = keystore_password;
		this.alias = alias;
	}

	public Path keystore_path() {
		return keystore_path;
	}

	public Path ensure_keystore() throws IOException, InterruptedException {
		if (Files.exists(keystore_path)) {
			return keystore_path;
		}
		Path parent = keystore_path.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		generate_self_signed_keystore();
		return keystore_path;
	}

	public SSLContext create_ssl_context() throws Exception {
		ensure_keystore();
		KeyStore key_store = KeyStore.getInstance("JKS");
		try (InputStream input = new FileInputStream(keystore_path.toFile())) {
			key_store.load(input, keystore_password);
		}
		KeyManagerFactory key_manager_factory = KeyManagerFactory.getInstance("SunX509");
		key_manager_factory.init(key_store, keystore_password);
		TrustManagerFactory trust_manager_factory = TrustManagerFactory.getInstance("SunX509");
		trust_manager_factory.init(key_store);
		SSLContext ssl_context = SSLContext.getInstance("TLS");
		ssl_context.init(
				key_manager_factory.getKeyManagers(),
				trust_manager_factory.getTrustManagers(),
				null);
		return ssl_context;
	}

	private void generate_self_signed_keystore() throws IOException, InterruptedException {
		List<String> command = new ArrayList<>();
		command.add("keytool");
		command.add("-genkeypair");
		command.add("-alias");
		command.add(alias);
		command.add("-keyalg");
		command.add("RSA");
		command.add("-keysize");
		command.add("2048");
		command.add("-validity");
		command.add("3650");
		command.add("-keystore");
		command.add(keystore_path.toString());
		command.add("-storepass");
		command.add(new String(keystore_password));
		command.add("-keypass");
		command.add(new String(keystore_password));
		command.add("-dname");
		command.add("CN=server_browser.org, OU=ServerBrowser, O=Honkord, L=Local, ST=Local, C=US");
		// PKIX DNS SAN labels allow only letters, digits, and hyphens (no underscores).
		command.add("-ext");
		command.add("SAN=dns:localhost,ip:127.0.0.1");

		Process process = new ProcessBuilder(command)
				.redirectErrorStream(true)
				.start();
		String keytool_output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		int exit_code = process.waitFor();
		if (exit_code != 0 || !Files.exists(keystore_path)) {
			String detail = keytool_output.isBlank() ? "keytool exit " + exit_code : keytool_output.trim();
			throw new IOException("Failed to generate HTTPS keystore at " + keystore_path + ": " + detail);
		}
		text_printer.print(text_printer.format.LOG, "Generated HTTPS keystore at " + keystore_path);
	}
}
