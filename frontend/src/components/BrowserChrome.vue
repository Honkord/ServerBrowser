<script setup lang="ts">
import { computed } from "vue";
import { hostnameFromUrl } from "../utils/url";

const props = defineProps<{
  address: string;
  signedIn: boolean;
  username: string;
  loading: boolean;
  canGoBack: boolean;
}>();

const emit = defineEmits<{
  navigate: [url: string];
  reload: [];
  back: [];
  account: [];
  newTab: [];
}>();

const localAddress = defineModel<string>("address", { required: true });

const tabTitle = computed(() => {
  if (!props.address) return "New Tab";
  return hostnameFromUrl(props.address);
});

const secureIcon = computed(() => (props.address.startsWith("https") ? "🔒" : "⚠️"));

function onAddressSubmit() {
  emit("navigate", localAddress.value);
}
</script>

<template>
  <header class="browser-chrome">
    <div class="window-controls" aria-hidden="true">
      <span class="dot close" />
      <span class="dot minimize" />
      <span class="dot maximize" />
    </div>
    <nav class="toolbar" aria-label="Browser navigation">
      <button type="button" class="icon-btn" title="Back" :disabled="!canGoBack" aria-label="Back" @click="emit('back')">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 18l-6-6 6-6" /></svg>
      </button>
      <button type="button" class="icon-btn" title="Forward" disabled aria-label="Forward">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 18l6-6-6-6" /></svg>
      </button>
      <button type="button" class="icon-btn" title="Reload" aria-label="Reload" @click="emit('reload')">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M17.65 6.35A7.96 7.96 0 0 0 12 4a8 8 0 1 0 8 8h-2a6 6 0 1 1-1.76-4.24L15 9H4V4l3.35 2.35z" />
        </svg>
      </button>
      <form class="address-bar" autocomplete="off" @submit.prevent="onAddressSubmit">
        <span class="secure-icon" :title="localAddress ? 'Connection' : ''">{{ secureIcon }}</span>
        <input v-model="localAddress" type="text" placeholder="Search or enter address" spellcheck="false" aria-label="Address" />
        <button type="submit" class="go-btn" aria-label="Go">Go</button>
      </form>
      <button type="button" class="icon-btn account-btn" title="Account" aria-label="Account" @click="emit('account')">
        {{ signedIn ? "Account" : "Sign in" }}
      </button>
    </nav>
    <div class="tab-strip" role="tablist" aria-label="Tabs">
      <button type="button" class="tab active" role="tab" aria-selected="true">
        <span class="tab-title">{{ tabTitle }}</span>
        <span class="tab-close" aria-hidden="true">×</span>
      </button>
      <button type="button" class="tab-add" title="New tab" aria-label="New tab" @click="emit('newTab')">+</button>
    </div>
  </header>
</template>
