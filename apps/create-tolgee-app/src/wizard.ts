import {
  cancel,
  confirm,
  intro,
  isCancel,
  multiselect,
  text,
} from '@clack/prompts'
import { existsSync } from 'node:fs'
import { createServer } from 'node:net'
import { resolve } from 'node:path'
import pc from 'picocolors'
import { MODULES, SCOPES, WEBHOOK_EVENTS, type ModuleKey } from './registry'

export type Answers = {
  id: string
  name: string
  targetDir: string
  selectedModules: ModuleKey[]
  scopes: string[]
  webhookEvents: string[]
  tolgeeUrl: string
  vitePort: number
  serverPort: number
  installDeps: boolean
  gitInit: boolean
}

const detectPackageManager = (): 'npm' | 'yarn' | 'pnpm' | 'bun' => {
  const ua = process.env.npm_config_user_agent ?? ''
  if (ua.startsWith('yarn')) return 'yarn'
  if (ua.startsWith('pnpm')) return 'pnpm'
  if (ua.startsWith('bun')) return 'bun'
  return 'npm'
}

const validateName = (raw: unknown): string | undefined => {
  if (typeof raw !== 'string' || raw.length === 0) return 'Name is required.'
  if (!/^[a-z][a-z0-9-]*$/.test(raw)) {
    return 'Use kebab-case: lowercase letters, digits, hyphens.'
  }
  if (existsSync(resolve(process.cwd(), raw))) {
    return `Directory "${raw}" already exists.`
  }
  return undefined
}

const abortIfCancelled = <T>(value: T | symbol): T => {
  if (isCancel(value)) {
    cancel('Cancelled.')
    process.exit(0)
  }
  return value as T
}

export async function runWizard(initialName?: string): Promise<Answers> {
  intro(pc.cyan('create-tolgee-app'))

  const id = abortIfCancelled(
    await text({
      message: 'App identifier (kebab-case — used as folder + manifest id):',
      initialValue: initialName,
      validate: validateName,
    })
  )

  const name = abortIfCancelled(
    await text({
      message: 'Display name (shown in Tolgee admin UI):',
      initialValue: toTitle(id),
      validate: (v) =>
        typeof v === 'string' && v.length > 0 ? undefined : 'Required.',
    })
  )

  const selectedModules = abortIfCancelled(
    await multiselect({
      message: 'Which modules should the plugin contribute?',
      required: true,
      options: MODULES.map((m) => ({
        value: m.key,
        label: m.label,
        hint: m.description,
      })),
      initialValues: ['project-dashboard-page'] satisfies ModuleKey[],
    })
  ) as ModuleKey[]

  const scopes = abortIfCancelled(
    await multiselect({
      message: 'Which permission scopes does the plugin need?',
      required: false,
      options: SCOPES.map((s) => ({ value: s.value, label: s.label })),
      initialValues: ['translations.view', 'keys.view'],
    })
  ) as string[]

  const webhookEvents = abortIfCancelled(
    await multiselect({
      message: 'Which webhook events should the plugin subscribe to?',
      required: false,
      options: WEBHOOK_EVENTS.map((e) => ({ value: e.value, label: e.label })),
      initialValues: [],
    })
  ) as string[]

  const tolgeeUrl = normalizeUrl(
    abortIfCancelled(
      await text({
        message: 'Tolgee URL:',
        initialValue: 'https://apps.preview.tolgee.io',
        validate: validateUrl,
      })
    )
  )

  const [freeVite, freeServer] = await findFreePortPair()
  const [vitePort, serverPort] = parsePortPair(
    abortIfCancelled(
      await text({
        message: 'Dev ports (Vite, Server) — auto-picked free pair, edit if you like:',
        initialValue: `${freeVite}, ${freeServer}`,
        validate: validatePortPair,
      })
    )
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
    targetDir: resolve(process.cwd(), id),
    selectedModules,
    scopes,
    webhookEvents,
    tolgeeUrl,
    vitePort,
    serverPort,
    installDeps,
    gitInit,
  }
}

/** True if a TCP port is free to bind on any interface (IPv4 + IPv6). */
const isPortFree = (port: number): Promise<boolean> =>
  new Promise((res) => {
    const srv = createServer()
    srv.once('error', () => res(false))
    srv.once('listening', () => srv.close(() => res(true)))
    srv.listen(port)
  })

/** First free (vite, server) pair, scanning 5180/5181, 5280/5281, … so plugins don't clash. */
export const findFreePortPair = async (): Promise<[number, number]> => {
  for (let base = 5180; base <= 5980; base += 100) {
    if ((await isPortFree(base)) && (await isPortFree(base + 1))) {
      return [base, base + 1]
    }
  }
  return [5180, 5181]
}

export const validatePortPair = (v: unknown): string | undefined => {
  if (typeof v !== 'string') return 'Required.'
  const parts = v.split(',').map((s) => s.trim())
  if (parts.length !== 2) return 'Give two ports separated by a comma, e.g. `5180, 5181`.'
  for (const p of parts) {
    const n = Number(p)
    if (!Number.isInteger(n) || n < 1 || n > 65535) return `"${p}" is not a valid port.`
  }
  if (parts[0] === parts[1]) return 'Vite and server ports must differ.'
  return undefined
}

/** Parses a validated `"vite, server"` string into a numeric pair. */
export const parsePortPair = (v: string): [number, number] => {
  const [vite, server] = v.split(',').map((s) => Number(s.trim()))
  return [vite, server]
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

export { detectPackageManager }
