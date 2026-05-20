/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md.
 */

package com.server_browser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Outbound fetcher using HTTP/2 (with HTTP/1.1 fallback), connection reuse, compression,
 * and async parallel requests.
 */
public final class fast_page_fetcher {
	public record fetch_result(
			int status_code,
			String content_type,
			byte[] body,
			String resolved_url) {
	}

	private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(4);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(12);

	private static final ExecutorService FETCH_EXECUTOR = Executors.newFixedThreadPool(
			24,
			new ThreadFactory() {
				private int count;

				@Override
				public Thread newThread(Runnable runnable) {
					Thread thread = new Thread(runnable, "browse-fetch-" + ++count);
					thread.setDaemon(true);
					return thread;
				}
			});

	private final HttpClient http;

	public fast_page_fetcher() {
		this.http = HttpClient.newBuilder()
				.connectTimeout(CONNECT_TIMEOUT)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.version(HttpClient.Version.HTTP_2)
				.executor(FETCH_EXECUTOR)
				.build();
	}

	public ExecutorService executor() {
		return FETCH_EXECUTOR;
	}

	public Optional<fetch_result> fetch(URI uri) {
		return fetch(uri, browse_resource_kind.DOCUMENT);
	}

	public Optional<fetch_result> fetch(URI uri, browse_resource_kind kind) {
		try {
			return Optional.of(send_request(uri, kind));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public CompletableFuture<Optional<fetch_result>> fetch_async(URI uri, browse_resource_kind kind) {
		HttpRequest request = build_request(uri, kind);
		return http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
				.handle((response, error) -> {
					if (error != null) {
						return Optional.<fetch_result>empty();
					}
					try {
						return Optional.of(to_result(response));
					} catch (Exception e) {
						return Optional.empty();
					}
				});
	}

	private fetch_result send_request(URI uri, browse_resource_kind kind) throws Exception {
		HttpResponse<byte[]> response = http.send(build_request(uri, kind), HttpResponse.BodyHandlers.ofByteArray());
		return to_result(response);
	}

	private HttpRequest build_request(URI uri, browse_resource_kind kind) {
		return HttpRequest.newBuilder(uri)
				.timeout(REQUEST_TIMEOUT)
				.header("User-Agent", "Mozilla/5.0 (compatible; ServerBrowser/1.0)")
				.header("Accept", accept_header(kind))
				.header("Accept-Language", "en-US,en;q=0.9")
				.header("Accept-Encoding", "gzip, deflate")
				.GET()
				.build();
	}

	private fetch_result to_result(HttpResponse<byte[]> response) {
		byte[] body = truncate(http_body_decoder.decode(response));
		String content_type = response.headers()
				.firstValue("Content-Type")
				.orElse(guess_content_type(response.uri()));
		return new fetch_result(
				response.statusCode(),
				content_type,
				body,
				response.uri().toString());
	}

	private static String accept_header(browse_resource_kind kind) {
		return switch (kind) {
			case DOCUMENT -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
			case STYLESHEET -> "text/css,*/*;q=0.8";
			case SCRIPT -> "*/*;q=0.8";
			case IMAGE -> "image/avif,image/webp,image/apng,image/*,*/*;q=0.8";
			case FONT -> "font/woff2,font/woff,font/ttf,*/*;q=0.8";
			case MEDIA -> "video/*,audio/*,*/*;q=0.8";
			case ANY -> "*/*";
		};
	}

	private static String guess_content_type(URI uri) {
		String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
		if (path.endsWith(".css")) {
			return "text/css; charset=utf-8";
		}
		if (path.endsWith(".js") || path.endsWith(".mjs")) {
			return "application/javascript; charset=utf-8";
		}
		if (path.endsWith(".png")) {
			return "image/png";
		}
		if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
			return "image/jpeg";
		}
		if (path.endsWith(".webp")) {
			return "image/webp";
		}
		if (path.endsWith(".svg")) {
			return "image/svg+xml";
		}
		if (path.endsWith(".woff2")) {
			return "font/woff2";
		}
		if (path.endsWith(".mp4")) {
			return "video/mp4";
		}
		return "application/octet-stream";
	}

	private static byte[] truncate(byte[] body) {
		if (body == null) {
			return new byte[0];
		}
		if (body.length <= MAX_BODY_BYTES) {
			return body;
		}
		byte[] limited = new byte[MAX_BODY_BYTES];
		System.arraycopy(body, 0, limited, 0, MAX_BODY_BYTES);
		return limited;
	}
}
