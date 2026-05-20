/** Injected into proxied pages so link clicks route through the app. */
const NAV_BRIDGE = `<script id="server-browser-nav-bridge">
(function () {
  function navigate(url) {
    if (window.parent && window.parent !== window) {
      window.parent.postMessage({ type: "server-browser-navigate", url: url }, "*");
    }
  }
  document.addEventListener(
    "click",
    function (e) {
      var el = e.target;
      if (!el || !el.closest) return;
      var anchor = el.closest("a[href]");
      if (!anchor) return;
      var href = anchor.getAttribute("href");
      if (!href || href.charAt(0) === "#" || href.indexOf("javascript:") === 0) return;
      e.preventDefault();
      e.stopPropagation();
      try {
        navigate(new URL(anchor.href, document.baseURI).href);
      } catch (err) {}
    },
    true
  );
  document.addEventListener(
    "submit",
    function (e) {
      var form = e.target;
      if (!form || form.tagName !== "FORM") return;
      e.preventDefault();
      try {
        var action = form.action || document.baseURI;
        var target = new URL(action, document.baseURI);
        if ((form.method || "GET").toUpperCase() === "GET") {
          var data = new FormData(form);
          data.forEach(function (value, key) {
            if (typeof value === "string") target.searchParams.set(key, value);
          });
        }
        navigate(target.href);
      } catch (err) {}
    },
    true
  );
})();
</script>`;

export function decodeResponseBytes(buffer: ArrayBuffer, contentType: string): string {
  const type = (contentType || "").toLowerCase();
  const match = type.match(/charset=([^;\s]+)/i);
  if (match) {
    try {
      return new TextDecoder(match[1].trim()).decode(buffer);
    } catch {
      /* utf-8 fallback */
    }
  }
  return new TextDecoder("utf-8").decode(buffer);
}

function escapeAttr(value: string): string {
  return value.replace(/&/g, "&amp;").replace(/"/g, "&quot;");
}

export function preparePageHtml(html: string, pageUrl: string): string {
  if (!html) {
    return "<!DOCTYPE html><html><head></head><body><p>Empty response</p></body></html>";
  }
  const baseTag = `<base href="${escapeAttr(pageUrl)}" target="_self">`;
  const bridge = NAV_BRIDGE;
  const lower = html.toLowerCase();
  if (lower.includes("<head>")) {
    return html.replace(/<head>/i, `<head>${baseTag}${bridge}`);
  }
  if (lower.includes("<html>")) {
    return html.replace(/<html>/i, `<html><head>${baseTag}${bridge}</head>`);
  }
  return `<!DOCTYPE html><html><head>${baseTag}${bridge}</head><body>${html}</body></html>`;
}

let activeBlobUrl: string | null = null;

export function revokeActiveBlob(): void {
  if (activeBlobUrl) {
    URL.revokeObjectURL(activeBlobUrl);
    activeBlobUrl = null;
  }
}

export function createPageBlobUrl(html: string, pageUrl: string): string {
  revokeActiveBlob();
  const doc = preparePageHtml(html, pageUrl);
  activeBlobUrl = URL.createObjectURL(new Blob([doc], { type: "text/html;charset=utf-8" }));
  return activeBlobUrl;
}
