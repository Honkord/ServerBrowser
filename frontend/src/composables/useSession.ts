import { computed, ref } from "vue";
import { STORAGE_TOKEN_KEY, STORAGE_USER_KEY } from "../api/config";
import { loginRequest, registerRequest, validateSessionRequest } from "../api/auth";
import { loadCertificatePin } from "../api/client";

const token = ref(sessionStorage.getItem(STORAGE_TOKEN_KEY) || "");
const username = ref(sessionStorage.getItem(STORAGE_USER_KEY) || "");

export function useSession() {
  const signedIn = computed(() => Boolean(token.value));

  function setSession(accessToken: string, name: string) {
    token.value = accessToken;
    username.value = name;
    sessionStorage.setItem(STORAGE_TOKEN_KEY, accessToken);
    sessionStorage.setItem(STORAGE_USER_KEY, name);
  }

  function clearSession() {
    token.value = "";
    username.value = "";
    sessionStorage.removeItem(STORAGE_TOKEN_KEY);
    sessionStorage.removeItem(STORAGE_USER_KEY);
  }

  async function login(name: string, password: string) {
    const session = await loginRequest(name, password);
    setSession(session.accessToken, session.username);
  }

  async function register(name: string, password: string) {
    await registerRequest(name, password);
    await login(name, password);
  }

  async function validateSession() {
    if (!token.value) {
      return false;
    }
    const valid = await validateSessionRequest(token.value);
    if (!valid) {
      clearSession();
    }
    return valid;
  }

  async function initSecurity() {
    await loadCertificatePin();
  }

  return {
    token,
    username,
    signedIn,
    setSession,
    clearSession,
    login,
    register,
    validateSession,
    initSecurity,
  };
}
