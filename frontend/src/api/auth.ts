import { API_BASE } from "./config";
import { secureFetch } from "./client";

export interface AuthSession {
  accessToken: string;
  username: string;
}

export type OAuthProvider = "google" | "microsoft" | "apple";

export interface OAuthProviderStatus {
  enabled: boolean;
  label: string;
  redirectUri: string;
}

export interface OAuthConfigResponse {
  configDir: string;
  oauthFile: string;
  setupHint: string;
  providers: Record<OAuthProvider, OAuthProviderStatus>;
}

export function oauthStartUrl(provider: OAuthProvider): string {
  return `${API_BASE}/api/auth/oauth/${provider}/start`;
}

export function startOAuthSignIn(provider: OAuthProvider): void {
  window.location.href = oauthStartUrl(provider);
}

export async function fetchOAuthConfig(): Promise<OAuthConfigResponse> {
  const response = await fetch(`${API_BASE}/api/auth/oauth/config`, { credentials: "same-origin" });
  if (!response.ok) {
    throw new Error("Unable to load OAuth configuration status");
  }
  return (await response.json()) as OAuthConfigResponse;
}

export async function loginRequest(username: string, password: string): Promise<AuthSession> {
  const response = await secureFetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error((data as { error?: string }).error || "Sign in failed");
  }
  const payload = data as { accessToken: string; username: string };
  return { accessToken: payload.accessToken, username: payload.username };
}

export async function registerRequest(username: string, password: string): Promise<void> {
  const response = await secureFetch(`${API_BASE}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  const data = (await response.json()) as { error?: string; code?: string };
  if (!response.ok) {
    if (data.code === "username_taken") {
      throw new Error("That username is already registered. Sign in instead.");
    }
    throw new Error(data.error || "Registration failed");
  }
}

export async function validateSessionRequest(token: string): Promise<boolean> {
  const response = await secureFetch(`${API_BASE}/api/auth/session`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.ok;
}
