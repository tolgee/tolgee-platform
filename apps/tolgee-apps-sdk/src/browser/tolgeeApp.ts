import type { TolgeeAppContext, TolgeeAppTheme } from '../shared/contextTypes'

type Unsubscribe = () => void

export type TolgeeAppOptions = {
  /**
   * Origin(s) of the Tolgee instance this app may talk to — a full URL or a bare
   * origin (`https://app.tolgee.io`). Set it whenever the app knows which Tolgee
   * embeds it: only then can a page that frames the app be rejected outright.
   *
   * Left unset, the first `tolgee-app:init` posted by the parent window is
   * trusted and its origin is pinned for the rest of the session — enough to
   * stop a second window from swapping the token mid-session, but not enough to
   * stop a hostile embedder, which is the parent by definition.
   *
   * Throws when a value is not a parsable URL, rather than silently degrading to
   * the weaker mode.
   */
  tolgeeOrigin?: string | string[]
}

type InitMessage = {
  type: 'tolgee-app:init'
  token: string
  apiUrl: string
  organizationId: number | null
  projectId: number
  theme: TolgeeAppTheme
}

type ThemeChangedMessage = {
  type: 'tolgee-app:theme-changed'
  theme: TolgeeAppTheme
}

/**
 * Iframe-side handle to the Tolgee Apps postMessage protocol.
 *
 * Construct via {@link createTolgeeApp}. Sends `tolgee-app:ready` to the
 * parent automatically on the next microtask, so you can attach listeners
 * before the host's init message arrives. The first accepted `tolgee-app:init`
 * resolves {@link TolgeeApp.context}; later `tolgee-app:theme-changed`
 * messages fire registered theme handlers.
 *
 * The init message carries an API token, so it is only accepted from the parent
 * window and — once `tolgeeOrigin` is set or the first init has arrived — only
 * from that one origin. Everything this app posts back goes to the same pinned
 * origin instead of `*`.
 */
export class TolgeeApp {
  private contextPromise: Promise<TolgeeAppContext>
  private resolveContext!: (ctx: TolgeeAppContext) => void
  private themeHandlers = new Set<(t: TolgeeAppTheme) => void>()
  private currentTheme: TolgeeAppTheme | undefined
  /** Origins the app declared up front; null when it declared none. */
  private allowedOrigins: string[] | null
  /** Origin of the accepted init, pinned for every later message. */
  private hostOrigin: string | null = null
  /** Window the accepted init came from, pinned alongside its origin. */
  private hostWindow: MessageEventSource | null = null

  constructor(options: TolgeeAppOptions = {}) {
    this.allowedOrigins = toOrigins(options.tolgeeOrigin)
    this.contextPromise = new Promise((resolve) => {
      this.resolveContext = resolve
    })
    window.addEventListener('message', this.onMessage)
    queueMicrotask(() => {
      this.postToHost({ type: 'tolgee-app:ready' })
    })
  }

  /**
   * Resolves with the init payload the host posted via `tolgee-app:init`.
   * Will never resolve outside a Tolgee iframe — test for
   * `window.parent !== window` before constructing if you need a fallback path.
   */
  get context(): Promise<TolgeeAppContext> {
    return this.contextPromise
  }

  /**
   * Subscribe to host theme changes (light/dark toggles). Returns an
   * unsubscribe function and fires once with the current theme after init.
   * Pair with `applyTolgeeTheme` to restyle the iframe live.
   */
  onThemeChanged(handler: (theme: TolgeeAppTheme) => void): Unsubscribe {
    this.themeHandlers.add(handler)
    if (this.currentTheme) handler(this.currentTheme)
    return () => {
      this.themeHandlers.delete(handler)
    }
  }

  /** Tells the host how tall this iframe wants to be. */
  resize(height: number): void {
    this.postToHost({ type: 'tolgee-app:resize', height })
  }

  /** Detaches the message listener. Safe to call multiple times. */
  dispose(): void {
    window.removeEventListener('message', this.onMessage)
  }

  private onMessage = (event: MessageEvent): void => {
    const d = event.data
    if (isInit(d)) {
      if (!this.acceptsInitFrom(event)) return
      this.hostOrigin = event.origin
      this.hostWindow = event.source
      const ctx = parseInit(d)
      this.currentTheme = ctx.theme
      this.resolveContext(ctx)
      // Deliver the initial theme to handlers registered before init arrived
      // (createTolgeeApp() is called, then handlers are added synchronously, all
      // before the host's init message). Without this, onThemeChanged would only
      // fire on later changes, never on first load.
      this.themeHandlers.forEach((h) => h(ctx.theme))
    } else if (isThemeChanged(d)) {
      if (!this.isFromHost(event)) return
      this.currentTheme = d.theme
      this.themeHandlers.forEach((h) => h(d.theme))
    }
  }

  /**
   * Only the embedder may hand this app a token, and only from an origin the app
   * either declared or already accepted an init from.
   */
  private acceptsInitFrom(event: MessageEvent): boolean {
    if (this.hostOrigin !== null) return this.isFromHost(event)
    if (event.source !== window.parent) return false
    return (
      this.allowedOrigins === null ||
      this.allowedOrigins.includes(event.origin)
    )
  }

  private isFromHost(event: MessageEvent): boolean {
    return event.origin === this.hostOrigin && event.source === this.hostWindow
  }

  /**
   * `ready` has to go out before any origin is pinned, so it is addressed to
   * every declared origin — or to `*` when the app declared none, which is safe
   * only because neither `ready` nor `resize` carries anything secret.
   */
  private postToHost(message: unknown): void {
    const targets =
      this.hostOrigin !== null ? [this.hostOrigin] : (this.allowedOrigins ?? ['*'])
    for (const target of targets) {
      window.parent.postMessage(message, target)
    }
  }
}

export const createTolgeeApp = (options?: TolgeeAppOptions): TolgeeApp =>
  new TolgeeApp(options)

const isInit = (d: unknown): d is InitMessage =>
  typeof d === 'object' &&
  d !== null &&
  (d as { type: unknown }).type === 'tolgee-app:init'

const isThemeChanged = (d: unknown): d is ThemeChangedMessage =>
  typeof d === 'object' &&
  d !== null &&
  (d as { type: unknown }).type === 'tolgee-app:theme-changed'

const parseInit = (m: InitMessage): TolgeeAppContext => ({
  token: m.token,
  apiUrl: m.apiUrl,
  organizationId: m.organizationId ?? null,
  projectId: m.projectId,
  theme: m.theme,
})

const toOrigins = (value: string | string[] | undefined): string[] | null => {
  if (value === undefined) return null
  // A comma-separated string is accepted because this usually arrives from an
  // environment variable, and Tolgee's web app and API are separate origins
  // unless one host serves both.
  const raw = (Array.isArray(value) ? value : value.split(','))
    .map((v) => v.trim())
    .filter((v) => v.length > 0)
  if (raw.length === 0) return null
  return raw.map((v) => {
    try {
      return new URL(v).origin
    } catch {
      throw new Error(
        `tolgeeOrigin must be an absolute URL or origin, got ${JSON.stringify(v)}.`
      )
    }
  })
}
