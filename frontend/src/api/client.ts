import { API_BASE, STORAGE_PIN_KEY } from "./config";

let pinReady = false;

export function assertSecureContext(): void {
  if (typeof window !== "undefined" && window.location.protocol !== "https:") {
    throw new Error("Secure connection required. Open the app over HTTPS.");
  }
}

export async function loadCertificatePin(): Promise<{ sha256Pin: string }> {
  assertSecureContext();
  const response = await fetch(`${API_BASE}/api/security/pin`, { credentials: "same-origin" });
  if (!response.ok) {
    throw new Error("Unable to load certificate pin");
  }
  const data = (await response.json()) as { sha256Pin: string };
  sessionStorage.setItem(STORAGE_PIN_KEY, data.sha256Pin);
  return data;
}

async function ensureCertificatePin(): Promise<void> {
  if (sessionStorage.getItem(STORAGE_PIN_KEY)) {
    return;
  }
  await loadCertificatePin();
}

export async function secureFetch(url: string, options: RequestInit = {}): Promise<Response> {
  assertSecureContext();
  if (!pinReady) {
    await ensureCertificatePin();
    pinReady = true;
  }
  const headers = new Headers(options.headers);
  if (!headers.has("Accept") && !String(url).includes("/api/browse")) {
    headers.set("Accept", "application/json");
  }
  return fetch(url, {
    ...options,
    headers,
    credentials: "same-origin",
    cache: "no-store",
    redirect: "error",
    referrerPolicy: "strict-origin-when-cross-origin",
  });
}
