import { existsSync } from 'node:fs'
import { resolve } from 'node:path'
import { cancel, confirm, intro, isCancel, note, text } from '@clack/prompts'
import pc from 'picocolors'
import { PACKAGE_ROOT } from './paths'
import {
  DEFAULT_SERVER_PORT,
  DEFAULT_TOLGEE_URL,
  DEFAULT_VITE_PORT,
  type SdkMode,
} from './registry'
import { resolveSdk, type SdkResolution } from './sdk'

export type Answers = {
  id: string
  name: string
  targetDir: string
  tolgeeUrl: string
  vitePort: number
  serverPort: number
  /** Which `@tolgee/apps-sdk` the generated app depends on. */
  sdk: SdkResolution
  installDeps: boolean
  gitInit: boolean
}

export async function runWizard(
  initialId: string | undefined,
  sdkMode: SdkMode
): Promise<Answers> {
  intro(pc.cyan('create-tolgee-app'))

  const id = abortIfCancelled(
    await text({
      message: 'App id (kebab-case — used as folder, package and manifest id):',
      initialValue: initialId,
      validate: validateId,
    })
  )

  const name = abortIfCancelled(
    await text({
      message: 'Display name (shown in Tolgee):',
      initialValue: toTitle(id),
      validate: (v) =>
        typeof v === 'string' && v.length > 0 ? undefined : 'Required.',
    })
  )

  const tolgeeUrl = normalizeUrl(
    abortIfCancelled(
      await text({
        message: 'Tolgee URL:',
        initialValue: DEFAULT_TOLGEE_URL,
        validate: validateUrl,
      })
    )
  )

  const targetDir = resolve(process.cwd(), id)
  const sdk = resolveSdk({ mode: sdkMode, targetDir, packageRoot: PACKAGE_ROOT })

  note(
    [
      `App        ${id} (${name})`,
      `Directory  ${targetDir}`,
      `Tolgee     ${tolgeeUrl}`,
      `SDK        ${sdk.summary}`,
    ].join('\n'),
    'Summary'
  )

  const pm = detectPackageManager()
  const installDeps = abortIfCancelled(
    await confirm({
      message: `Install dependencies now with ${pm}?`,
      initialValue: true,
    })
  )

  const gitInit = abortIfCancelled(
    await confirm({
      message: 'Initialize a git repo and make the initial commit?',
      initialValue: true,
    })
  )

  return {
    id,
    name,
    targetDir,
    tolgeeUrl,
    vitePort: DEFAULT_VITE_PORT,
    serverPort: DEFAULT_SERVER_PORT,
    sdk,
    installDeps,
    gitInit,
  }
}

export const validateId = (raw: unknown): string | undefined => {
  if (typeof raw !== 'string' || raw.length === 0) return 'App id is required.'
  if (!/^[a-z][a-z0-9-]*$/.test(raw)) {
    return 'Use kebab-case: lowercase letters, digits, hyphens.'
  }
  if (existsSync(resolve(process.cwd(), raw))) {
    return `Directory "${raw}" already exists.`
  }
  return undefined
}

export const validateUrl = (v: unknown): string | undefined => {
  if (typeof v !== 'string' || v.length === 0) return 'Required.'
  if ((v.match(/:\/\//g) ?? []).length > 1) {
    return 'URL has a duplicated scheme (e.g. http://http://…).'
  }
  let url: URL
  try {
    url = new URL(v)
  } catch {
    return 'Not a valid URL.'
  }
  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    return 'Must start with http:// or https://.'
  }
  return undefined
}

export const normalizeUrl = (v: string): string => v.trim().replace(/\/+$/, '')

export const toTitle = (slug: string): string => {
  return slug
    .split('-')
    .map((s) => s.charAt(0).toUpperCase() + s.slice(1))
    .join(' ')
}

export const packageManagerCommand = (
  pm: ReturnType<typeof detectPackageManager>,
  ...args: string[]
): string => `${pm} ${args.join(' ')}`

export const detectPackageManager = (): 'npm' | 'yarn' | 'pnpm' | 'bun' => {
  const ua = process.env.npm_config_user_agent ?? ''
  if (ua.startsWith('yarn')) return 'yarn'
  if (ua.startsWith('pnpm')) return 'pnpm'
  if (ua.startsWith('bun')) return 'bun'
  return 'npm'
}

const abortIfCancelled = <T>(value: T | symbol): T => {
  if (isCancel(value)) {
    cancel('Cancelled.')
    process.exit(0)
  }
  return value as T
}
