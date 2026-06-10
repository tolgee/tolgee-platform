import type {
  TolgeeAppContext,
  TolgeeAppSelection,
} from '../shared/contextTypes'

type Unsubscribe = () => void

const KNOWN_INIT_FIELDS = new Set([
  'type',
  'token',
  'apiUrl',
  'organizationId',
  'projectId',
  'keyId',
  'languageId',
  'translationId',
  'languageTag',
  'selectedLanguages',
])

type InitMessage = {
  type: 'tolgee-app:init'
  token: string
  apiUrl: string
  organizationId: number
  projectId: number
  keyId?: number
  languageId?: number
  languageTag?: string
  translationId?: number
  selectedLanguages?: string[]
} & Record<string, unknown>

type SelectionChangedMessage = {
  type: 'tolgee-app:selection-changed'
  keyId?: number
  languageId?: number
  languageTag?: string
  translationId?: number
  selectedLanguages?: string[]
}

const isInit = (d: unknown): d is InitMessage =>
  typeof d === 'object' &&
  d !== null &&
  (d as { type: unknown }).type === 'tolgee-app:init'

const isSelectionChanged = (d: unknown): d is SelectionChangedMessage =>
  typeof d === 'object' &&
  d !== null &&
  (d as { type: unknown }).type === 'tolgee-app:selection-changed'

const parseInit = (m: InitMessage): TolgeeAppContext => {
  const extra: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(m)) {
    if (!KNOWN_INIT_FIELDS.has(k)) extra[k] = v
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
      selectedLanguages: m.selectedLanguages,
    },
    extra,
  }
}

/**
 * Iframe-side handle to the Tolgee Apps postMessage protocol.
 *
 * Construct via `createTolgeeApp()`. Sends `tolgee-app:ready` to the
 * parent automatically on the next microtask, so you can attach
 * listeners before the host's init message arrives. The first init
 * resolves `context`; subsequent selection changes fire registered
 * selection handlers.
 */
export class TolgeeApp {
  private contextPromise: Promise<TolgeeAppContext>
  private resolveContext!: (ctx: TolgeeAppContext) => void
  private selectionHandlers = new Set<(s: TolgeeAppSelection) => void>()
  private currentSelection: TolgeeAppSelection = {}

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
   * Resolves with the init payload the host posted via
   * `tolgee-app:init`. Will never resolve outside a Tolgee iframe —
   * test for `window.parent !== window` before constructing if you
   * need a fallback path.
   */
  get context(): Promise<TolgeeAppContext> {
    return this.contextPromise
  }

  /**
   * Subscribe to selection changes (the host posts these when the user
   * focuses a different key/translation cell). Returns an unsubscribe
   * function. The handler also fires once with the initial selection
   * after init.
   */
  onSelectionChanged(
    handler: (selection: TolgeeAppSelection) => void
  ): Unsubscribe {
    this.selectionHandlers.add(handler)
    if (Object.values(this.currentSelection).some((v) => v !== undefined)) {
      handler(this.currentSelection)
    }
    return () => {
      this.selectionHandlers.delete(handler)
    }
  }

  /** Asks the host to close the modal/panel containing this iframe. */
  close(): void {
    window.parent.postMessage({ type: 'tolgee-app:close' }, '*')
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
      this.currentSelection = ctx.selection
      this.resolveContext(ctx)
    } else if (isSelectionChanged(d)) {
      this.currentSelection = {
        keyId: d.keyId,
        languageId: d.languageId,
        languageTag: d.languageTag,
        translationId: d.translationId,
        selectedLanguages: d.selectedLanguages,
      }
      this.selectionHandlers.forEach((h) => h(this.currentSelection))
    }
  }
}

export const createTolgeeApp = (): TolgeeApp => new TolgeeApp()
