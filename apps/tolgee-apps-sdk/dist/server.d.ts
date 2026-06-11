import { WebhookType, AppWebhookPayload, WebhookPayloadFor } from './index.js';
export { AppAction, AppActionType, AppIframeModule, AppManifest, AppModules, AppShortcut, AppWebhooks, DecoratorItem, DecoratorsRequest, DecoratorsResponse } from './index.js';
import { A as AppContextClaims } from './contextTypes-xgD-1LAp.js';
import '@tginternal/client';

/**
 * Verifies the `Tolgee-Signature` HMAC header against the raw POST body.
 * Returns true when the signature matches the per-install webhook
 * secret. Uses WebCrypto so the same code runs on Node 20+, Cloudflare
 * Workers, Bun, and Deno.
 *
 * The caller is responsible for reading the raw request body before any
 * JSON parser sees it — Express's `express.text({ type: 'application/json' })`
 * is the standard way.
 *
 *     const ok = await verifyWebhookSignature({
 *       header: req.header('Tolgee-Signature'),
 *       rawBody: req.body,
 *       secret: process.env.TOLGEE_WEBHOOK_SECRET!,
 *     })
 */
declare function verifyWebhookSignature(opts: {
    header: string | null | undefined;
    rawBody: string;
    secret: string;
}): Promise<boolean>;

/**
 * If `payload` matches one of the supplied activity `types`, invokes
 * `handler` with the payload narrowed to that specific type's schema.
 * Returns true when the handler ran.
 *
 *     onWebhook(payload, 'SET_TRANSLATIONS', (p) => {
 *       // p: WebhookPayloadFor<'SET_TRANSLATIONS'>
 *       p.activityData?.modifiedEntities?.Translation?.forEach(...)
 *     })
 *
 *     onWebhook(payload, ['SET_TRANSLATIONS', 'BATCH_SET_TRANSLATION_STATE'], (p) => {
 *       // p: WebhookPayloadFor<'SET_TRANSLATIONS' | 'BATCH_SET_TRANSLATION_STATE'>
 *     })
 */
declare function onWebhook<T extends WebhookType>(payload: AppWebhookPayload, types: T | readonly T[], handler: (typed: WebhookPayloadFor<T>) => void): boolean;

type ReceiveWebhookInput = {
    /** The exact bytes Tolgee signed — do NOT pass a re-serialized object. */
    rawBody: string;
    /** Value of the `Tolgee-Signature` header. */
    signatureHeader: string | null | undefined;
    /**
     * Per-install webhook secret. When null/undefined, signature verification
     * is skipped — only acceptable in local development.
     */
    secret: string | null | undefined;
};
type ReceiveWebhookResult = {
    ok: true;
    payload: AppWebhookPayload;
} | {
    ok: false;
    status: 401 | 400;
    error: string;
};
/**
 * Framework-agnostic webhook intake: verifies the `Tolgee-Signature` over the
 * raw body and parses it into a typed payload. Returns a result the caller maps
 * to its HTTP framework; on success, dispatch with `onWebhook`.
 *
 *     const r = await receiveWebhook({ rawBody, signatureHeader, secret })
 *     if (!r.ok) return res.status(r.status).json({ error: r.error })
 *     onWebhook(r.payload, 'SET_TRANSLATIONS', (p) => { ... })
 */
declare function receiveWebhook(input: ReceiveWebhookInput): Promise<ReceiveWebhookResult>;

/**
 * Decodes (does NOT cryptographically verify) the Tolgee-issued context
 * JWT into typed claims. Plugin backends generally don't have access to
 * the platform's signing key, so verification happens server-side at
 * Tolgee — passing the same token as a bearer on REST calls back to
 * Tolgee is what authenticates the plugin.
 *
 * Use this when you need the install/user/project ids out of the token
 * for logging, routing, or persistence.
 */
declare const decodeContextToken: (jwt: string) => AppContextClaims;

/**
 * CORS headers a Tolgee App's server endpoints (decorators, custom APIs) must
 * return so the webapp — served from a different origin than the app — can call
 * them. Wildcard origin is safe here because the install token travels in the
 * `Authorization` header, not a credentialed cookie.
 *
 * Apply them in any framework, e.g. Express:
 *     for (const [k, v] of Object.entries(tolgeeAppCorsHeaders())) res.setHeader(k, v)
 */
declare const tolgeeAppCorsHeaders: () => Record<string, string>;

/**
 * Substitutes the `__BASE_URL__` placeholder in a manifest template with the
 * app's currently-reachable base URL (a tunnel URL in dev, the deployed origin
 * in production). Keeping the placeholder in the stored template lets the URL
 * change between dev restarts without editing the manifest.
 */
declare const renderManifest: (template: string, baseUrl: string) => string;

type TolgeeAppConfig = {
    /** Base URL of the Tolgee instance the app is installed on. */
    tolgeeUrl: string;
    /** Per-install webhook secret; null when unset (dev: signatures unverified). */
    webhookSecret: string | null;
    /** Vite dev-server port (iframe assets). */
    vitePort: number;
    /** App server port (manifest/webhook/decorator routes). */
    serverPort: number;
};
/**
 * Reads a Tolgee App's standard environment variables into a typed config.
 * Centralizes the env-var contract (names + defaults) so every app reads them
 * the same way.
 */
declare const loadTolgeeAppConfig: (env?: NodeJS.ProcessEnv) => TolgeeAppConfig;

export { AppContextClaims, AppWebhookPayload, type ReceiveWebhookInput, type ReceiveWebhookResult, type TolgeeAppConfig, WebhookPayloadFor, WebhookType, decodeContextToken, loadTolgeeAppConfig, onWebhook, receiveWebhook, renderManifest, tolgeeAppCorsHeaders, verifyWebhookSignature };
