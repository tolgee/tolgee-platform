import { webhooks } from '@tginternal/client';
export { A as AppContextClaims, T as TolgeeAppContext, a as TolgeeAppSelection } from './contextTypes-xgD-1LAp.js';

/**
 * Identifier of a webhook event Tolgee can send — keys of the `webhooks`
 * interface from the generated OpenAPI schema. Examples: `"SET_TRANSLATIONS"`,
 * `"BATCH_KEY_RESTORE"`, `"CREATE_KEY"`.
 */
type WebhookType = keyof webhooks;
/**
 * Request-body type for a given webhook event.
 *
 *     type SetTranslationsPayload = WebhookPayloadFor<'SET_TRANSLATIONS'>
 */
type WebhookPayloadFor<T extends WebhookType> = webhooks[T]['post']['requestBody']['content']['application/json'];
/**
 * Discriminated union of every typed webhook payload Tolgee can send.
 * Use this as the parsed type for the raw POST body before dispatching
 * via `onWebhook` from `@tolgee/apps-sdk/server`.
 */
type AppWebhookPayload = {
    [K in WebhookType]: WebhookPayloadFor<K>;
}[WebhookType];

/**
 * Typed model of a Tolgee App manifest. This is the single source of truth
 * shared by `create-tolgee-app`'s builder, the generated `manifest.template.json`,
 * and hand-written apps. The JSON keys (incl. kebab-case module names) match
 * exactly what the platform's `AppManifestFetcher` parses and validates.
 */
type AppManifest = {
    id: string;
    name: string;
    version: string;
    baseUrl: string;
    /** Endpoint the webapp POSTs to for dynamic row decorators. */
    decoratorsUrl?: string;
    /** Tolgee permission scopes the app requests at install time (e.g. `keys.edit`). */
    scopes?: string[];
    webhooks?: AppWebhooks;
    modules?: AppModules;
};
type AppWebhooks = {
    events: WebhookType[];
    /** Relative to `baseUrl` or absolute. */
    url: string;
};
/**
 * The action surface kind. Trigger surfaces (bulk/toolbar/menu/shortcut) accept
 * only `link` and `modal`; `key-action` additionally accepts `tab`; `translation-action`
 * additionally accepts `panel`. The platform rejects invalid combinations at register time.
 */
type AppActionType = 'link' | 'tab' | 'panel' | 'modal';
/** An iframe-bearing module (dashboard page, panel, key-edit tab, modal). */
type AppIframeModule = {
    key: string;
    title: string;
    /** Named icon from the platform icon set (e.g. `LayoutAlt04`). */
    icon?: string;
    /** Route the iframe loads, relative to `baseUrl`. */
    entry: string;
    /** Modal-only: iframe pixel size. */
    width?: number;
    height?: number;
};
/** An action that triggers a link, an iframe module, or a modal. */
type AppAction = {
    key: string;
    type: AppActionType;
    icon?: string;
    tooltip?: string;
    title?: string;
    /** `link` actions: target URL, may contain placeholders like `{keyId}`. */
    urlTemplate?: string;
    /** `tab` actions: which `key-edit-tab` to open. */
    tabKey?: string;
    /** `panel` actions: which `translation-tools-panel` to open. */
    panelKey?: string;
    /** `modal` actions: which `modal` to open. */
    modalKey?: string;
    /**
     * When true, visibility/decoration of this action is driven by the app's
     * `decoratorsUrl` response rather than shown statically on every row.
     */
    dynamic?: boolean;
};
/** A global keyboard-shortcut binding that triggers a link or modal. */
type AppShortcut = Omit<AppAction, 'type'> & {
    /** e.g. `Mod+Shift+E`. */
    combination: string;
    type: Extract<AppActionType, 'link' | 'modal'>;
};
/**
 * App modules keyed by the platform's kebab-case module identifiers. Every
 * key is optional; an app contributes only the surfaces it needs.
 */
type AppModules = {
    'project-dashboard-page'?: AppIframeModule[];
    'translation-tools-panel'?: AppIframeModule[];
    /** Panel shown in the translations tools area when no cell is being edited. */
    'translation-tools-panel-empty'?: AppIframeModule[];
    'key-edit-tab'?: AppIframeModule[];
    modal?: AppIframeModule[];
    'key-action'?: AppAction[];
    'translation-action'?: AppAction[];
    'bulk-action'?: AppAction[];
    'translations-toolbar-action'?: AppAction[];
    'project-menu-action'?: AppAction[];
    shortcut?: AppShortcut[];
};

/**
 * Contract for the dynamic-decorators endpoint (`manifest.decoratorsUrl`).
 * The Tolgee webapp POSTs a [DecoratorsRequest] describing the rows currently
 * visible; the app replies with a [DecoratorsResponse] of icon decorations to
 * render alongside native row icons.
 */
type DecoratorsRequest = {
    installId: number;
    projectId: number;
    /** Key rows in view. */
    keyIds?: number[];
    /** Language columns in view, by tag. */
    languageTags?: string[];
};
/** One decoration the app wants rendered on a key (or key+language) cell. */
type DecoratorItem = {
    keyId: number;
    /** Omit for a key-level decoration; set for a translation-cell decoration. */
    languageTag?: string;
    /** Named icon from the platform icon set. */
    icon: string;
    tooltip?: string;
};
type DecoratorsResponse = {
    items: DecoratorItem[];
};

export type { AppAction, AppActionType, AppIframeModule, AppManifest, AppModules, AppShortcut, AppWebhookPayload, AppWebhooks, DecoratorItem, DecoratorsRequest, DecoratorsResponse, WebhookPayloadFor, WebhookType };
