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
  "languageTag"
]);
var isInit = (d) => typeof d === "object" && d !== null && d.type === "tolgee-app:init";
var isSelectionChanged = (d) => typeof d === "object" && d !== null && d.type === "tolgee-app:selection-changed";
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
      translationId: m.translationId
    },
    extra
  };
};
var TolgeeApp = class {
  contextPromise;
  resolveContext;
  selectionHandlers = /* @__PURE__ */ new Set();
  currentSelection = {};
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
      this.resolveContext(ctx);
    } else if (isSelectionChanged(d)) {
      this.currentSelection = {
        keyId: d.keyId,
        languageId: d.languageId,
        languageTag: d.languageTag,
        translationId: d.translationId
      };
      this.selectionHandlers.forEach((h) => h(this.currentSelection));
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
export {
  TolgeeApp,
  createTolgeeApp,
  createTolgeeAppClient
};
//# sourceMappingURL=browser.js.map