export type ContentViewKind = "html" | "text" | "image" | "video" | "audio" | "download";

export function contentViewKind(contentType: string, contentClass: string, bodyBytes: ArrayBuffer): ContentViewKind {
  const type = (contentType || "").toLowerCase();
  const klass = (contentClass || "").toLowerCase();
  if (klass.includes("media_image") || type.startsWith("image/")) {
    return "image";
  }
  if (klass.includes("media_video") || type.startsWith("video/")) {
    return "video";
  }
  if (klass.includes("media_audio") || type.startsWith("audio/")) {
    return "audio";
  }
  if (klass.includes("download") || type.includes("pdf") || type.includes("zip") || type.includes("octet-stream")) {
    if (!looksTextual(bodyBytes)) {
      return "download";
    }
  }
  if (type.includes("text/html") || klass === "text") {
    return "html";
  }
  if (type.startsWith("text/") || type.includes("json") || type.includes("javascript")) {
    return "text";
  }
  if (!looksTextual(bodyBytes)) {
    return "download";
  }
  return "text";
}

function looksTextual(buffer: ArrayBuffer): boolean {
  if (!buffer.byteLength) {
    return true;
  }
  const bytes = new Uint8Array(buffer.slice(0, Math.min(buffer.byteLength, 512)));
  for (const value of bytes) {
    if (value === 9 || value === 10 || value === 13) {
      continue;
    }
    if (value < 32 || value === 127) {
      return false;
    }
  }
  return true;
}

export function filenameFromUrl(url: string): string {
  try {
    const path = new URL(url).pathname;
    const name = path.split("/").pop();
    return name && name.length > 0 ? name : "download";
  } catch {
    return "download";
  }
}
