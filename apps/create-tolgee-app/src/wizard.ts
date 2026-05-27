import {
  cancel,
  confirm,
  intro,
  isCancel,
  multiselect,
  text,
} from '@clack/prompts'
import { existsSync } from 'node:fs'
import { resolve } from 'node:path'
import pc from 'picocolors'
import { MODULES, WEBHOOK_EVENTS, type ModuleKey } from './registry'

export type Answers = {
  id: string
  name: string
  targetDir: string
  selectedModules: ModuleKey[]
  webhookEvents: string[]
  tolgeeUrl: string
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

  const webhookEvents = abortIfCancelled(
    await multiselect({
      message: 'Which webhook events should the plugin subscribe to?',
      required: false,
      options: WEBHOOK_EVENTS.map((e) => ({ value: e.value, label: e.label })),
      initialValues: [],
    })
  ) as string[]

  const tolgeeUrl = abortIfCancelled(
    await text({
      message: 'Tolgee URL:',
      initialValue: 'https://app.tolgee.io',
      validate: (v) => {
        if (typeof v !== 'string' || v.length === 0) return 'Required.'
        try {
          new URL(v)
          return undefined
        } catch {
          return 'Not a valid URL.'
        }
      },
    })
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
    webhookEvents,
    tolgeeUrl,
    installDeps,
    gitInit,
  }
}

const toTitle = (slug: string): string => {
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
