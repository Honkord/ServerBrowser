<script setup lang="ts">
import type { DownloadView, MediaView } from "../composables/useBrowse";

defineProps<{
  loading: boolean;
  error: string;
  frameSrc: string;
  rawText: string;
  viewMode: "start" | "html" | "raw" | "media" | "download";
  pageUrl: string;
  mediaView: MediaView | null;
  downloadView: DownloadView | null;
}>();

function formatSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
</script>

<template>
  <section class="content-panel" aria-live="polite">
    <div v-if="loading" class="loading-overlay">
      <p class="loading">Connecting…</p>
      <p v-if="pageUrl" class="loading-url">{{ pageUrl }}</p>
    </div>

    <iframe
      v-if="viewMode === 'html' && frameSrc"
      :key="frameSrc"
      class="page-frame"
      :class="{ 'page-frame--loading': loading }"
      title="Page content"
      sandbox="allow-same-origin allow-scripts allow-forms allow-popups"
      :src="frameSrc"
    />

    <div v-else-if="viewMode === 'media' && mediaView && !loading" class="media-viewer">
      <img
        v-if="mediaView.kind === 'image'"
        class="media-viewer__image"
        :src="mediaView.blobUrl"
        :alt="pageUrl"
      />
      <video
        v-else-if="mediaView.kind === 'video'"
        class="media-viewer__video"
        controls
        autoplay
        :src="mediaView.blobUrl"
      />
      <audio
        v-else-if="mediaView.kind === 'audio'"
        class="media-viewer__audio"
        controls
        autoplay
        :src="mediaView.blobUrl"
      />
    </div>

    <div v-else-if="viewMode === 'download' && downloadView && !loading" class="download-panel">
      <h2>File ready</h2>
      <p class="download-panel__meta">
        <strong>{{ downloadView.filename }}</strong>
        <span>{{ downloadView.contentType }}</span>
        <span>{{ formatSize(downloadView.size) }}</span>
      </p>
      <p>This file type is not rendered in the browser. Save it to your device.</p>
      <a class="download-panel__button" :href="downloadView.blobUrl" :download="downloadView.filename">
        Download file
      </a>
    </div>

    <pre v-else-if="viewMode === 'raw' && rawText && !loading" class="raw-view">{{ rawText }}</pre>

    <div v-else-if="!loading && !error && viewMode === 'html'" class="page-placeholder">
      <p>Waiting for page content…</p>
    </div>

    <p v-if="error" class="error-banner" role="alert">{{ error }}</p>
  </section>
</template>
