import { API_BASE } from "./config";
import { secureFetch } from "./client";

export interface BrowseResult {
  contentType: string;
  contentClass: string;
  statusCode: number;
  resolvedUrl: string;
  bodyBytes: ArrayBuffer;
}

let activeController: AbortController | null = null;

export function cancelBrowse(): void {
  activeController?.abort();
  activeController = null;
}

export async function fetchPage(url: string, token: string): Promise<BrowseResult> {
  cancelBrowse();
  activeController = new AbortController();
  const endpoint = `${API_BASE}/api/browse?url=${encodeURIComponent(url)}`;
  const response = await secureFetch(endpoint, {
    method: "GET",
    signal: activeController.signal,
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "text/html,application/xhtml+xml,*/*",
    },
  });
  if (!response.ok) {
    const err = (await response.json().catch(() => ({}))) as { error?: string };
    throw new Error(err.error || `Request failed (${response.status})`);
  }
  const contentType = response.headers.get("Content-Type") || "application/octet-stream";
  const contentClass = response.headers.get("X-Browse-Content-Class") || "";
  const statusCode = Number.parseInt(response.headers.get("X-Browse-Status") || "200", 10);
  const resolvedUrl = response.headers.get("X-Browse-Resolved-Url") || url;
  const bodyBytes = await response.arrayBuffer();
  return { contentType, contentClass, statusCode, resolvedUrl, bodyBytes };
}
