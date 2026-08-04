import type { TolgeeAppContext, TolgeeAppTheme } from '../shared/contextTypes'

type Unsubscribe = () => void

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

/**
 * Iframe-side handle to the Tolgee Apps postMessage protocol.
 *
 * Construct via {@link createTolgeeApp}. Sends `tolgee-app:ready` to the
 * parent automatically on the next microtask, so you can attach listeners
 * before the host's init message arrives. The first `tolgee-app:init`
 * resolves {@link TolgeeApp.context}; later `tolgee-app:theme-changed`
 * messages fire registered theme handlers.
 */
export class TolgeeApp {
  private contextPromise: Promise<TolgeeAppContext>
  private resolveContext!: (ctx: TolgeeAppContext) => void
  private themeHandlers = new Set<(t: TolgeeAppTheme) => void>()
  private currentTheme: TolgeeAppTheme | undefined

  constructor() {
    this.contextPromise = new Promise((resolve) => {
      this.resolveContext = resolve
    })
    window.addEventListener('message', this.onMessage)
    queueMicrotask(() => {
      window.parent.postMessage({ type: 'tolgee-app:ready' }, '*')
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
    window.parent.postMessage({ type: 'tolgee-app:resize', height }, '*')
  }

  /** Detaches the message listener. Safe to call multiple times. */
  dispose(): void {
    window.removeEventListener('message', this.onMessage)
  }

  private onMessage = (event: MessageEvent): void => {
    const d = event.data
    if (isInit(d)) {
      const ctx = parseInit(d)
      this.currentTheme = ctx.theme
      this.resolveContext(ctx)
      // Deliver the initial theme to handlers registered before init arrived
      // (createTolgeeApp() is called, then handlers are added synchronously, all
      // before the host's init message). Without this, onThemeChanged would only
      // fire on later changes, never on first load.
      this.themeHandlers.forEach((h) => h(ctx.theme))
    } else if (isThemeChanged(d)) {
      this.currentTheme = d.theme
      this.themeHandlers.forEach((h) => h(d.theme))
    }
  }
}

export const createTolgeeApp = (): TolgeeApp => new TolgeeApp()
