/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DNS phonebook for the {@code server_browser.org} zone.
 */
public final class server_browser_org_phonebook {
	public static final String ZONE = "server_browser.org";
	public static Path seed_file() {
		return application_paths.dns_phonebook_seed();
	}

	public record phonebook_resolution(
			String query,
			String record_name,
			String record_type,
			String target,
			Integer port,
			Optional<String> resolved_url) {
	}

	private static final Pattern HOST_PATTERN = Pattern.compile(
			"^(?:([a-z0-9-]+)\\.)?server_browser\\.org$",
			Pattern.CASE_INSENSITIVE);

	private final dns_phonebook_repository repository;

	public server_browser_org_phonebook(dns_phonebook_repository repository) {
		this.repository = repository;
	}

	public static void seed_from_file(dns_phonebook_repository repository) throws IOException, SQLException {
		Path seed = seed_file();
		if (!Files.exists(seed)) {
			return;
		}
		String json = Files.readString(seed);
		for (String block : json.split("\\{")) {
			if (!block.contains("\"name\"")) {
				continue;
			}
			String name = extract_json_string(block, "name");
			String type = extract_json_string(block, "type");
			String target = extract_json_string(block, "target");
			if (name == null || type == null || target == null) {
				continue;
			}
			Integer port = extract_json_int(block, "port");
			repository.upsert(ZONE, name, type.toUpperCase(), target, port, 300);
		}
		text_printer.print(text_printer.format.LOG, "DNS phonebook seeded for " + ZONE);
	}

	public Optional<phonebook_resolution> resolve_host(String host_or_url) throws SQLException {
		if (host_or_url == null || host_or_url.isBlank()) {
			return Optional.empty();
		}
		String host = host_or_url.trim();
		if (host.contains("://")) {
			host = URI.create(host).getHost();
		}
		if (host == null) {
			return Optional.empty();
		}
		host = host.toLowerCase();
		if (!host.endsWith(ZONE) && !host.equals(ZONE)) {
			return Optional.empty();
		}
		String record_name = record_name_from_host(host);
		Optional<dns_phonebook_repository.phonebook_record> record = repository.find(ZONE, record_name);
		if (record.isEmpty() && !"@".equals(record_name)) {
			record = repository.find(ZONE, "@");
		}
		if (record.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(to_resolution(host, record.get()));
	}

	public List<dns_phonebook_repository.phonebook_record> list_records() throws SQLException {
		return repository.list_zone(ZONE);
	}

	private phonebook_resolution to_resolution(String query, dns_phonebook_repository.phonebook_record record)
			throws SQLException {
		String type = record.record_type().toUpperCase();
		return switch (type) {
			case "ALIAS" -> new phonebook_resolution(
					query,
					record.record_name(),
					type,
					record.target(),
					record.port(),
					Optional.of(record.target()));
			case "CNAME" -> resolve_cname(query, record);
			case "A", "AAAA" -> new phonebook_resolution(
					query,
					record.record_name(),
					type,
					record.target(),
					record.port(),
					Optional.of(build_url(record.target(), record.port())));
			default -> new phonebook_resolution(
					query,
					record.record_name(),
					type,
					record.target(),
					record.port(),
					Optional.empty());
		};
	}

	private phonebook_resolution resolve_cname(
			String query,
			dns_phonebook_repository.phonebook_record record) throws SQLException {
		String target = record.target();
		if (target.endsWith("." + ZONE) || target.equals(ZONE)) {
			String chained_name = record_name_from_host(target.toLowerCase());
			Optional<dns_phonebook_repository.phonebook_record> chained = repository.find(ZONE, chained_name);
			if (chained.isPresent()) {
				return to_resolution(query, chained.get());
			}
		}
		return new phonebook_resolution(
				query,
				record.record_name(),
				"CNAME",
				target,
				record.port(),
				Optional.of(target.startsWith("http") ? target : "https://" + target));
	}

	private static String record_name_from_host(String host) {
		if (host.equals(ZONE)) {
			return "@";
		}
		Matcher matcher = HOST_PATTERN.matcher(host);
		if (matcher.matches() && matcher.group(1) != null) {
			return matcher.group(1).toLowerCase();
		}
		String prefix = host.substring(0, host.length() - ZONE.length() - 1);
		return prefix.isEmpty() ? "@" : prefix.toLowerCase();
	}

	private static String build_url(String target, Integer port) {
		if (target.startsWith("http://") || target.startsWith("https://")) {
			return target;
		}
		int resolved_port = port == null ? 443 : port;
		String scheme = resolved_port == 443 ? "https" : "http";
		return scheme + "://" + target + (port != null && port != 80 && port != 443 ? ":" + port : "");
	}

	private static String extract_json_string(String block, String key) {
		Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
		Matcher matcher = pattern.matcher(block);
		return matcher.find() ? matcher.group(1) : null;
	}

	private static Integer extract_json_int(String block, String key) {
		Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
		Matcher matcher = pattern.matcher(block);
		return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
	}
}
