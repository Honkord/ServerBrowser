<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import AuthDialog from "./components/AuthDialog.vue";
import BrowserChrome from "./components/BrowserChrome.vue";
import PageView from "./components/PageView.vue";
import StartPage from "./components/StartPage.vue";
import { useBrowse } from "./composables/useBrowse";
import { useSession } from "./composables/useSession";
import { normalizeUrl } from "./utils/url";

const session = useSession();
const { token, username, signedIn, login, register, validateSession, initSecurity, clearSession } = session;

const {
  loading,
  error,
  frameSrc,
  rawText,
  viewMode,
  mediaView,
  downloadView,
  statusCode,
  currentUrl,
  navigate: browseNavigate,
  showStart,
  cancelBrowse,
} = useBrowse(() => token.value);

const address = ref("");
const search = ref("");
const statusText = ref("Ready");
const authOpen = ref(false);
const authMode = ref<"login" | "register">("login");
const authDialog = ref<InstanceType<typeof AuthDialog> | null>(null);

const history = ref<string[]>([]);
const historyIndex = ref(-1);

function setStatus(text: string) {
  statusText.value = text;
}

function pushHistory(url: string) {
  if (historyIndex.value < history.value.length - 1) {
    history.value = history.value.slice(0, historyIndex.value + 1);
  }
  if (history.value[history.value.length - 1] !== url) {
    history.value.push(url);
  }
  historyIndex.value = history.value.length - 1;
}

async function go(input: string) {
  if (!signedIn.value) {
    authOpen.value = true;
    setStatus("Sign in to browse");
    return;
  }

  const url = normalizeUrl(input.trim());
  if (!url) return;

  address.value = url;
  search.value = url;
  setStatus(`Connecting to ${url}…`);

  try {
    const result = await browseNavigate(url);
    if (!result) {
      setStatus("Cancelled");
      return;
    }
    address.value = currentUrl.value;
    search.value = currentUrl.value;
    pushHistory(currentUrl.value);
    setStatus(`Loaded ${currentUrl.value} (${statusCode.value})`);
  } catch {
    setStatus(error.value || "Error");
  }
}

function onIframeNavigate(event: MessageEvent) {
  if (event.data?.type !== "server-browser-navigate" || typeof event.data.url !== "string") {
    return;
  }
  go(event.data.url).catch(() => undefined);
}

function onAccount() {
  if (signedIn.value) {
    clearSession();
    showStart();
    address.value = "";
    search.value = "";
    setStatus("Signed out");
    return;
  }
  authMode.value = "login";
  authOpen.value = true;
}

async function onAuthSubmit(name: string, password: string) {
  try {
    if (authMode.value === "register") {
      await register(name, password);
    } else {
      await login(name, password);
    }
    authOpen.value = false;
    setStatus("Signed in — enter a URL to browse");
  } catch (err) {
    authDialog.value?.setError(err instanceof Error ? err.message : "Authentication failed");
  }
}

function onBack() {
  if (historyIndex.value > 0) {
    historyIndex.value -= 1;
    go(history.value[historyIndex.value]).catch(() => undefined);
  }
}

function onNewTab() {
  cancelBrowse();
  showStart();
  address.value = "";
  search.value = "";
  setStatus("New tab");
}

onMounted(async () => {
  window.addEventListener("message", onIframeNavigate);
  try {
    await initSecurity();
  } catch (err) {
    setStatus(`Security pin unavailable: ${err instanceof Error ? err.message : "error"}`);
  }
  const valid = await validateSession();
  setStatus(valid ? "Ready — enter a URL or search term" : "Sign in to browse");

  const params = new URLSearchParams(window.location.search);
  const oauthToken = params.get("oauthToken");
  const oauthUser = params.get("oauthUser");
  if (oauthToken && oauthUser) {
    session.setSession(oauthToken, oauthUser);
    setStatus(`Signed in as ${oauthUser}`);
    window.history.replaceState({}, "", window.location.pathname);
  }

  const pending = params.get("url");
  if (pending && (valid || oauthToken)) {
    go(pending).catch(() => undefined);
  } else if (pending) {
    setStatus(`Sign in to open ${params.get("phonebook") || pending}`);
  }
});

onUnmounted(() => {
  window.removeEventListener("message", onIframeNavigate);
  cancelBrowse();
});
</script>

<template>
  <div class="browser-app">
    <BrowserChrome
      v-model:address="address"
      :signed-in="signedIn"
      :username="username"
      :loading="loading"
      :can-go-back="historyIndex > 0"
      @navigate="go"
      @reload="go(address)"
      @back="onBack"
      @account="onAccount"
      @new-tab="onNewTab"
    />

    <main class="browser-main">
      <StartPage v-if="viewMode === 'start'" v-model:search="search" @navigate="go" />
      <PageView
        v-else
        :loading="loading"
        :error="error"
        :frame-src="frameSrc"
        :raw-text="rawText"
        :view-mode="viewMode"
        :page-url="currentUrl"
        :media-view="mediaView"
        :download-view="downloadView"
      />
    </main>

    <footer class="status-bar">
      <span>{{ statusText }}</span>
      <span class="session-indicator" :class="{ 'signed-in': signedIn }">
        {{ signedIn ? username || "Signed in" : "Guest" }}
      </span>
    </footer>

    <AuthDialog
      ref="authDialog"
      v-model:open="authOpen"
      :mode="authMode"
      @toggle-mode="authMode = authMode === 'login' ? 'register' : 'login'"
      @submit="onAuthSubmit"
    />
  </div>
</template>
