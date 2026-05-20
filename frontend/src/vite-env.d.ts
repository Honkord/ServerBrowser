/// <reference types="vite/client" />

declare global {
  interface Window {
    __SERVER_BROWSER_APP_URL__?: string;
  }
}

export {};
