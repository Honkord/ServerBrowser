import { PHONEBOOK_ZONE } from "../api/config";

const BLOCKED_SCHEMES = ["javascript:", "data:", "file:"];

export function normalizeUrl(input: string): string | null {
  const trimmed = (input || "").trim();
  if (!trimmed) {
    return null;
  }
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }
  if (trimmed.endsWith(`.${PHONEBOOK_ZONE}`) || trimmed === PHONEBOOK_ZONE) {
    return `https://${trimmed}`;
  }
  if (trimmed.includes(".") && !trimmed.includes(" ")) {
    return `https://${trimmed}`;
  }
  return `https://www.google.com/search?q=${encodeURIComponent(trimmed)}`;
}

export function isAllowedUrl(url: string): boolean {
  try {
    const parsed = new URL(url);
    return !BLOCKED_SCHEMES.includes(parsed.protocol) && (parsed.protocol === "http:" || parsed.protocol === "https:");
  } catch {
    return false;
  }
}

export function hostnameFromUrl(url: string): string {
  try {
    return new URL(url).hostname;
  } catch {
    return url;
  }
}
