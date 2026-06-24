import type { TolgeeAppTheme } from '../shared/contextTypes'

const VAR_PREFIX = '--tg-color-'

const kebab = (s: string): string =>
  s.replace(/[A-Z]/g, (m) => `-${m.toLowerCase()}`)

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
export const applyTolgeeTheme = (
  theme: TolgeeAppTheme,
  root: HTMLElement = document.documentElement
): void => {
  for (const [name, value] of Object.entries(theme.colors)) {
    root.style.setProperty(`${VAR_PREFIX}${kebab(name)}`, value)
  }
  root.dataset.tgTheme = theme.mode
  root.style.colorScheme = theme.mode
}
