import { ref } from "vue";
import { cancelBrowse, fetchPage, type BrowseResult } from "../api/browse";
import { createPageBlobUrl, decodeResponseBytes, revokeActiveBlob } from "../utils/pageContent";
import { contentViewKind, filenameFromUrl, type ContentViewKind } from "../utils/contentKind";
import { isAllowedUrl, normalizeUrl } from "../utils/url";

export interface MediaView {
  kind: "image" | "video" | "audio";
  blobUrl: string;
  contentType: string;
}

export interface DownloadView {
  blobUrl: string;
  filename: string;
  contentType: string;
  size: number;
}

export function useBrowse(getToken: () => string) {
  const loading = ref(false);
  const error = ref("");
  const currentUrl = ref("");
  const frameSrc = ref("");
  const rawText = ref("");
  const viewMode = ref<"start" | "html" | "raw" | "media" | "download">("start");
  const contentKind = ref<ContentViewKind>("html");
  const mediaView = ref<MediaView | null>(null);
  const downloadView = ref<DownloadView | null>(null);
  const statusCode = ref(200);

  let mediaBlobUrl: string | null = null;
  let downloadBlobUrl: string | null = null;

  function clearMediaBlob() {
    if (mediaBlobUrl) {
      URL.revokeObjectURL(mediaBlobUrl);
      mediaBlobUrl = null;
    }
  }

  function clearDownloadBlob() {
    if (downloadBlobUrl) {
      URL.revokeObjectURL(downloadBlobUrl);
      downloadBlobUrl = null;
    }
  }

  function beginLoading(url: string) {
    error.value = "";
    loading.value = true;
    viewMode.value = "html";
    revokeActiveBlob();
    clearMediaBlob();
    clearDownloadBlob();
    frameSrc.value = "";
    rawText.value = "";
    mediaView.value = null;
    downloadView.value = null;
    currentUrl.value = url;
  }

  function showStart() {
    cancelBrowse();
    loading.value = false;
    error.value = "";
    viewMode.value = "start";
    revokeActiveBlob();
    clearMediaBlob();
    clearDownloadBlob();
    frameSrc.value = "";
    rawText.value = "";
    mediaView.value = null;
    downloadView.value = null;
    currentUrl.value = "";
  }

  function presentResult(result: BrowseResult) {
    const kind = contentViewKind(result.contentType, result.contentClass, result.bodyBytes);
    contentKind.value = kind;

    if (kind === "html") {
      const body = decodeResponseBytes(result.bodyBytes, result.contentType);
      frameSrc.value = createPageBlobUrl(body, result.resolvedUrl);
      viewMode.value = "html";
      rawText.value = "";
      mediaView.value = null;
      downloadView.value = null;
      return;
    }

    revokeActiveBlob();
    frameSrc.value = "";
    rawText.value = "";

    if (kind === "image" || kind === "video" || kind === "audio") {
      clearMediaBlob();
      const type = result.contentType || `${kind === "image" ? "image/png" : kind === "video" ? "video/mp4" : "audio/mpeg"}`;
      mediaBlobUrl = URL.createObjectURL(new Blob([result.bodyBytes], { type }));
      mediaView.value = { kind, blobUrl: mediaBlobUrl, contentType: type };
      downloadView.value = null;
      viewMode.value = "media";
      return;
    }

    if (kind === "download") {
      clearMediaBlob();
      clearDownloadBlob();
      const type = result.contentType || "application/octet-stream";
      downloadBlobUrl = URL.createObjectURL(new Blob([result.bodyBytes], { type }));
      downloadView.value = {
        blobUrl: downloadBlobUrl,
        filename: filenameFromUrl(result.resolvedUrl),
        contentType: type,
        size: result.bodyBytes.byteLength,
      };
      mediaView.value = null;
      viewMode.value = "download";
      return;
    }

    clearMediaBlob();
    mediaView.value = null;
    downloadView.value = null;
    rawText.value = decodeResponseBytes(result.bodyBytes, result.contentType);
    viewMode.value = "raw";
  }

  async function navigate(input: string): Promise<BrowseResult | null> {
    const url = normalizeUrl(input);
    if (!url) {
      return null;
    }
    if (!getToken()) {
      throw new Error("Sign in required");
    }
    if (!isAllowedUrl(url)) {
      throw new Error("URL is not allowed.");
    }

    beginLoading(url);

    try {
      const result = await fetchPage(url, getToken());
      currentUrl.value = result.resolvedUrl;
      statusCode.value = result.statusCode;
      presentResult(result);
      loading.value = false;
      return result;
    } catch (err) {
      loading.value = false;
      if (err instanceof DOMException && err.name === "AbortError") {
        return null;
      }
      error.value = err instanceof Error ? err.message : "Navigation failed";
      viewMode.value = "html";
      throw err;
    }
  }

  return {
    loading,
    error,
    currentUrl,
    frameSrc,
    rawText,
    viewMode,
    contentKind,
    mediaView,
    downloadView,
    statusCode,
    navigate,
    showStart,
    cancelBrowse,
  };
}
