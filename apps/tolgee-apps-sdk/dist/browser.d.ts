import { T as TolgeeAppContext, a as TolgeeAppSelection, b as TolgeeAppTheme } from './contextTypes-xgD-1LAp.js';
import { createApiClient } from '@tginternal/client';

type Unsubscribe = () => void;
/**
 * Iframe-side handle to the Tolgee Apps postMessage protocol.
 *
 * Construct via `createTolgeeApp()`. Sends `tolgee-app:ready` to the
 * parent automatically on the next microtask, so you can attach
 * listeners before the host's init message arrives. The first init
 * resolves `context`; subsequent selection changes fire registered
 * selection handlers.
 */
declare class TolgeeApp {
    private contextPromise;
    private resolveContext;
    private selectionHandlers;
    private currentSelection;
    private themeHandlers;
    private currentTheme;
    constructor();
    /**
     * Resolves with the init payload the host posted via
     * `tolgee-app:init`. Will never resolve outside a Tolgee iframe —
     * test for `window.parent !== window` before constructing if you
     * need a fallback path.
     */
    get context(): Promise<TolgeeAppContext>;
    /**
     * Subscribe to selection changes (the host posts these when the user
     * focuses a different key/translation cell). Returns an unsubscribe
     * function. The handler also fires once with the initial selection
     * after init.
     */
    onSelectionChanged(handler: (selection: TolgeeAppSelection) => void): Unsubscribe;
    /**
     * Subscribe to host theme changes (light/dark toggles). Returns an
     * unsubscribe function and fires once with the current theme after init.
     * Pair with `applyTolgeeTheme` to restyle the iframe live.
     */
    onThemeChanged(handler: (theme: TolgeeAppTheme) => void): Unsubscribe;
    /** Asks the host to close the modal/panel containing this iframe. */
    close(): void;
    /** Tells the host how tall this iframe wants to be. */
    resize(height: number): void;
    /** Detaches the message listener. Safe to call multiple times. */
    dispose(): void;
    private onMessage;
}
declare const createTolgeeApp: () => TolgeeApp;

/**
 * Builds a typed Tolgee REST client wired with the install-context
 * token, base URL, and project id from the iframe's
 * `TolgeeAppContext`. Errors are returned in the `error` field rather
 * than thrown, matching `createApiClient`'s `autoThrow: false` mode.
 *
 *     const app = createTolgeeApp()
 *     const ctx = await app.context
 *     const tolgee = createTolgeeAppClient(ctx)
 *     const { data, error } = await tolgee.GET('/v2/projects/{projectId}', {
 *       params: { path: { projectId: ctx.projectId } },
 *     })
 */
declare const createTolgeeAppClient: (context: TolgeeAppContext) => ReturnType<typeof createApiClient>;

/**
 * Applies the host theme to the iframe document so the plugin matches Tolgee:
 * exposes each palette color as a `--tg-color-<name>` CSS custom property
 * (e.g. `--tg-color-background`, `--tg-color-text-secondary`), sets
 * `[data-tg-theme="light|dark"]`, and sets `color-scheme` (so native form
 * controls and scrollbars follow the mode).
 *
 * Call it once with `ctx.theme` and again from `onThemeChanged` to follow live
 * light/dark toggles:
 *
 * ```ts
 * const app = createTolgeeApp()
 * const ctx = await app.context
 * applyTolgeeTheme(ctx.theme)
 * app.onThemeChanged(applyTolgeeTheme)
 * ```
 */
declare const applyTolgeeTheme: (theme: TolgeeAppTheme, root?: HTMLElement) => void;

export { TolgeeApp, TolgeeAppContext, TolgeeAppSelection, TolgeeAppTheme, applyTolgeeTheme, createTolgeeApp, createTolgeeAppClient };
