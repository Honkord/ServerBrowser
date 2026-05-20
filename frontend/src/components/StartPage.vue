<script setup lang="ts">
const appUrl = typeof window !== "undefined" ? window.location.origin : "https://localhost:8443";

const emit = defineEmits<{
  navigate: [url: string];
}>();

const quickLinks = [
  { label: "Google", url: "https://www.google.com" },
  { label: "Example.com", url: "https://example.com" },
  { label: "Wikipedia", url: "https://www.wikipedia.org" },
  { label: "Example (phonebook)", url: "example.server_browser.org" },
];

const search = defineModel<string>("search", { required: true });

function onSearchSubmit() {
  emit("navigate", search.value);
}
</script>

<template>
  <section class="start-page">
    <h1 class="logo">Server<span>Browser</span></h1>
    <p class="tagline">Dynamic-Port Proxy / Web Fetch Gateway</p>
    <form class="search-box" autocomplete="off" @submit.prevent="onSearchSubmit">
      <input v-model="search" type="text" placeholder="Search the web or type a URL" aria-label="Search" />
      <button type="submit">Search</button>
    </form>
    <div class="phonebook-warning">
      <strong>Do not type <code>example.server_browser.org</code> in your system browser’s address bar</strong>
      — that name is not on the public internet.
      <ul>
        <li>Open this app: <strong>{{ appUrl }}</strong></li>
        <li>Sign in, then use the <strong>search box below</strong> for phonebook names</li>
        <li>Optional: run <code>scripts/install_phonebook_hosts.ps1</code> (Admin)</li>
      </ul>
    </div>
    <div class="quick-links">
      <button v-for="link in quickLinks" :key="link.url" type="button" @click="emit('navigate', link.url)">
        {{ link.label }}
      </button>
    </div>
  </section>
</template>
