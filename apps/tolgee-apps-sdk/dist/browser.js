// src/browser/tolgeeApp.ts
var KNOWN_INIT_FIELDS = /* @__PURE__ */ new Set([
  "type",
  "token",
  "apiUrl",
  "organizationId",
  "projectId",
  "keyId",
  "languageId",
  "translationId",
  "languageTag",
  "selectedLanguages",
  "theme"
]);
var isInit = (d) => typeof d === "object" && d !== null && d.type === "tolgee-app:init";
var isSelectionChanged = (d) => typeof d === "object" && d !== null && d.type === "tolgee-app:selection-changed";
var isThemeChanged = (d) => typeof d === "object" && d !== null && d.type === "tolgee-app:theme-changed";
var parseInit = (m) => {
  const extra = {};
  for (const [k, v] of Object.entries(m)) {
    if (!KNOWN_INIT_FIELDS.has(k)) extra[k] = v;
  }
  return {
    token: m.token,
    apiUrl: m.apiUrl,
    organizationId: m.organizationId,
    projectId: m.projectId,
    selection: {
      keyId: m.keyId,
      languageId: m.languageId,
      languageTag: m.languageTag,
      translationId: m.translationId,
      selectedLanguages: m.selectedLanguages
    },
    theme: m.theme,
    extra
  };
};
var TolgeeApp = class {
  contextPromise;
  resolveContext;
  selectionHandlers = /* @__PURE__ */ new Set();
  currentSelection = {};
  themeHandlers = /* @__PURE__ */ new Set();
  currentTheme;
  constructor() {
    this.contextPromise = new Promise((resolve) => {
      this.resolveContext = resolve;
    });
    window.addEventListener("message", this.onMessage);
    queueMicrotask(() => {
      window.parent.postMessage({ type: "tolgee-app:ready" }, "*");
    });
  }
  /**
   * Resolves with the init payload the host posted via
   * `tolgee-app:init`. Will never resolve outside a Tolgee iframe —
   * test for `window.parent !== window` before constructing if you
   * need a fallback path.
   */
  get context() {
    return this.contextPromise;
  }
  /**
   * Subscribe to selection changes (the host posts these when the user
   * focuses a different key/translation cell). Returns an unsubscribe
   * function. The handler also fires once with the initial selection
   * after init.
   */
  onSelectionChanged(handler) {
    this.selectionHandlers.add(handler);
    if (Object.values(this.currentSelection).some((v) => v !== void 0)) {
      handler(this.currentSelection);
    }
    return () => {
      this.selectionHandlers.delete(handler);
    };
  }
  /**
   * Subscribe to host theme changes (light/dark toggles). Returns an
   * unsubscribe function and fires once with the current theme after init.
   * Pair with `applyTolgeeTheme` to restyle the iframe live.
   */
  onThemeChanged(handler) {
    this.themeHandlers.add(handler);
    if (this.currentTheme) handler(this.currentTheme);
    return () => {
      this.themeHandlers.delete(handler);
    };
  }
  /** Asks the host to close the modal/panel containing this iframe. */
  close() {
    window.parent.postMessage({ type: "tolgee-app:close" }, "*");
  }
  /** Tells the host how tall this iframe wants to be. */
  resize(height) {
    window.parent.postMessage({ type: "tolgee-app:resize", height }, "*");
  }
  /** Detaches the message listener. Safe to call multiple times. */
  dispose() {
    window.removeEventListener("message", this.onMessage);
  }
  onMessage = (event) => {
    const d = event.data;
    if (isInit(d)) {
      const ctx = parseInit(d);
      this.currentSelection = ctx.selection;
      this.currentTheme = ctx.theme;
      this.resolveContext(ctx);
      this.selectionHandlers.forEach((h) => h(ctx.selection));
      this.themeHandlers.forEach((h) => h(ctx.theme));
    } else if (isSelectionChanged(d)) {
      this.currentSelection = {
        keyId: d.keyId,
        languageId: d.languageId,
        languageTag: d.languageTag,
        translationId: d.translationId,
        selectedLanguages: d.selectedLanguages
      };
      this.selectionHandlers.forEach((h) => h(this.currentSelection));
    } else if (isThemeChanged(d)) {
      this.currentTheme = d.theme;
      this.themeHandlers.forEach((h) => h(d.theme));
    }
  };
};
var createTolgeeApp = () => new TolgeeApp();

// src/browser/client.ts
import { createApiClient } from "@tginternal/client";
var createTolgeeAppClient = (context) => {
  return createApiClient({
    baseUrl: context.apiUrl,
    userToken: context.token,
    projectId: context.projectId,
    autoThrow: false
  });
};

// src/browser/applyTheme.ts
var VAR_PREFIX = "--tg-color-";
var kebab = (s) => s.replace(/[A-Z]/g, (m) => `-${m.toLowerCase()}`);
var applyTolgeeTheme = (theme, root = document.documentElement) => {
  for (const [name, value] of Object.entries(theme.colors)) {
    root.style.setProperty(`${VAR_PREFIX}${kebab(name)}`, value);
  }
  root.dataset.tgTheme = theme.mode;
  root.style.colorScheme = theme.mode;
};
export {
  TolgeeApp,
  applyTolgeeTheme,
  createTolgeeApp,
  createTolgeeAppClient
};
//# sourceMappingURL=browser.js.map