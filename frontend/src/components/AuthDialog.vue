<script setup lang="ts">
import { ref, watch } from "vue";
import {
  fetchOAuthConfig,
  startOAuthSignIn,
  type OAuthConfigResponse,
  type OAuthProvider,
} from "../api/auth";

const open = defineModel<boolean>("open", { required: true });

defineProps<{
  mode: "login" | "register";
}>();

const emit = defineEmits<{
  submit: [username: string, password: string];
  toggleMode: [];
}>();

const dialogEl = ref<HTMLDialogElement | null>(null);
const username = ref("");
const password = ref("");
const error = ref("");
const oauthConfig = ref<OAuthConfigResponse | null>(null);
const oauthLoading = ref(false);

const providers: OAuthProvider[] = ["google", "microsoft", "apple"];

watch(open, async (visible) => {
  const el = dialogEl.value;
  if (!el) return;
  if (visible && !el.open) {
    el.showModal();
    await loadOAuthConfig();
  } else if (!visible && el.open) {
    el.close();
  }
});

async function loadOAuthConfig() {
  oauthLoading.value = true;
  error.value = "";
  try {
    oauthConfig.value = await fetchOAuthConfig();
  } catch (err) {
    oauthConfig.value = null;
    error.value = err instanceof Error ? err.message : "Could not load sign-in options";
  } finally {
    oauthLoading.value = false;
  }
}

function onSubmit() {
  error.value = "";
  emit("submit", username.value.trim(), password.value);
}

function onOAuth(provider: OAuthProvider) {
  error.value = "";
  const status = oauthConfig.value?.providers[provider];
  if (status && !status.enabled) {
    error.value = oauthConfig.value?.setupHint || `${status.label} sign-in is not configured on the server.`;
    return;
  }
  startOAuthSignIn(provider);
}

function setError(message: string) {
  error.value = message;
}

defineExpose({ setError });
</script>

<template>
  <dialog ref="dialogEl" class="auth-dialog" @close="open = false">
    <form class="auth-form" @submit.prevent="onSubmit">
      <h2>{{ mode === "login" ? "Sign in" : "Create account" }}</h2>

      <p v-if="oauthLoading" class="oauth-hint">Loading sign-in options…</p>

      <div v-else class="oauth-buttons">
        <button
          v-for="provider in providers"
          :key="provider"
          type="button"
          class="oauth-btn"
          :class="`oauth-btn--${provider}`"
          :disabled="!oauthConfig?.providers[provider]?.enabled"
          @click="onOAuth(provider)"
        >
          Continue with {{ oauthConfig?.providers[provider]?.label || provider }}
        </button>
      </div>

      <p v-if="oauthConfig && !oauthLoading" class="oauth-hint">
        Corporate sign-in is configured in
        <code>{{ oauthConfig.oauthFile }}</code
        >. Disabled providers need client IDs and secrets added there.
      </p>

      <p class="auth-divider">or use a local account</p>

      <label>
        Username
        <input v-model="username" type="text" required autocomplete="username" />
      </label>
      <label>
        Password
        <input v-model="password" type="password" required autocomplete="current-password" />
      </label>
      <p v-if="error" class="auth-error">{{ error }}</p>
      <div class="auth-actions">
        <button type="button" @click="emit('toggleMode')">
          {{ mode === "login" ? "Create account" : "Sign in" }}
        </button>
        <button type="submit" class="primary">Continue</button>
      </div>
    </form>
  </dialog>
</template>
