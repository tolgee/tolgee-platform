import { spawnSync } from 'node:child_process'
import { existsSync, readFileSync } from 'node:fs'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { log, note, outro, spinner } from '@clack/prompts'
import pc from 'picocolors'
import { copyTree } from './copy'
import { buildManifest } from './manifest'
import { MODULES, type ModuleKey } from './registry'
import {
  detectPackageManager,
  findFreePortPair,
  normalizeUrl,
  packageManagerCommand,
  runWizard,
  toTitle,
  type Answers,
} from './wizard'

const __dirname = dirname(fileURLToPath(import.meta.url))

/**
 * tsup bundles a single dist/index.js next to the project root. The
 * template/ folder ships under the package root, so we walk up one
 * level from dist/.
 */
const PACKAGE_ROOT = resolve(__dirname, '..')
const TEMPLATE_ROOT = join(PACKAGE_ROOT, 'template')

/**
 * Version to pin the generated app's `@tolgee/*` deps to. In the monorepo the
 * apps-sdk/apps-dev packages sit next to this one, so we link them as workspace
 * deps (`*`); when published, those siblings are absent and we pin to this CLI's
 * own version (apps-sdk + apps-dev are released in lockstep). Detection is by
 * sibling presence, not version number, so it survives release bumps.
 */
const internalDepVersion = (): string => {
  const inRepo = existsSync(resolve(PACKAGE_ROOT, '..', 'tolgee-apps-sdk', 'package.json'))
  if (inRepo) return '*'
  try {
    const pkg = JSON.parse(
      readFileSync(join(PACKAGE_ROOT, 'package.json'), 'utf8')
    ) as { version?: string }
    return pkg.version ?? '*'
  } catch {
    return '*'
  }
}

const MODULE_ENTRY_PATHS: Record<ModuleKey, { route: string; importPath: string }> = {
  'project-dashboard-page': { route: '/dashboard', importPath: './modules/dashboard' },
  'translation-tools-panel': { route: '/tools-panel', importPath: './modules/toolsPanel' },
  'translation-tools-panel-empty': { route: '/tools-panel-empty', importPath: './modules/toolsPanelEmpty' },
  'key-edit-tab': { route: '/key-edit-tab', importPath: './modules/keyEditTab' },
  modal: { route: '/modal', importPath: './modules/modal' },
  'key-action': { route: '', importPath: '' },
  'translation-action': { route: '', importPath: '' },
  'bulk-action': { route: '', importPath: '' },
  'translations-toolbar-action': { route: '', importPath: '' },
  'project-menu-action': { route: '', importPath: '' },
  shortcut: { route: '', importPath: '' },
}

const main = async (): Promise<void> => {
  const answers = await resolveAnswers(process.argv.slice(2))

  if (existsSync(answers.targetDir)) {
    log.error(`Target ${pc.bold(answers.targetDir)} already exists.`)
    process.exit(1)
  }

  await scaffold(answers)

  const pm = detectPackageManager()
  const next: string[] = [
    `cd ${answers.id}`,
    answers.installDeps ? null : packageManagerCommand(pm, 'install'),
    `${packageManagerCommand(pm, 'run', 'dev')}        # terminal 1: vite + server + tunnel`,
    `${packageManagerCommand(pm, 'run', 'register')}   # terminal 2, first run only: browser install`,
  ].filter((s): s is string => s !== null)
  note(next.join('\n'), 'Next steps')
  outro(pc.cyan(`${answers.name} is ready.`))
}

/**
 * Resolves answers either from the interactive wizard or, when `--yes`/`-y` is
 * passed, from CLI flags + defaults (headless mode for CI and scripted scaffolds).
 *
 *     create-tolgee-app my-app --yes \
 *       --modules=project-dashboard-page,key-action \
 *       --scopes=translations.view,keys.edit --webhooks=SET_TRANSLATIONS
 */
const resolveAnswers = async (argv: string[]): Promise<Answers> => {
  const positional = argv.find((a) => !a.startsWith('-'))
  const flags = new Map(
    argv
      .filter((a) => a.startsWith('--'))
      .map((a) => {
        const [k, ...v] = a.slice(2).split('=')
        return [k, v.join('=')] as const
      })
  )
  const nonInteractive = flags.has('yes') || argv.includes('-y')

  if (!nonInteractive) {
    return runWizard(positional)
  }

  const id = positional ?? flags.get('name')
  if (!id || !/^[a-z][a-z0-9-]*$/.test(id)) {
    log.error('Headless mode (--yes) needs a kebab-case app name, e.g. `create-tolgee-app my-app --yes`.')
    process.exit(1)
  }

  const csv = (key: string): string[] | undefined => {
    const raw = flags.get(key)
    if (raw === undefined) return undefined
    return raw.split(',').map((s) => s.trim()).filter(Boolean)
  }

  const selectedModules = (csv('modules') ?? ['project-dashboard-page']) as ModuleKey[]
  const unknown = selectedModules.filter((m) => !MODULES.some((info) => info.key === m))
  if (unknown.length > 0) {
    log.error(`Unknown module(s): ${unknown.join(', ')}`)
    process.exit(1)
  }

  return {
    id,
    name: flags.get('name') && flags.get('name') !== id ? (flags.get('name') as string) : toTitle(id),
    targetDir: resolve(process.cwd(), id),
    selectedModules,
    scopes: csv('scopes') ?? ['translations.view', 'keys.view'],
    webhookEvents: csv('webhooks') ?? [],
    tolgeeUrl: normalizeUrl(flags.get('tolgee-url') ?? 'https://apps.preview.tolgee.io'),
    ...(await resolvePorts(flags)),
    installDeps: flags.has('install'),
    gitInit: flags.has('git'),
  }
}

/** Headless port resolution: explicit flags win, otherwise auto-pick a free pair. */
const resolvePorts = async (
  flags: Map<string, string>
): Promise<{ vitePort: number; serverPort: number }> => {
  const [freeVite, freeServer] = await findFreePortPair()
  const vitePort = flags.has('vite-port') ? Number(flags.get('vite-port')) : freeVite
  const serverPort = flags.has('server-port')
    ? Number(flags.get('server-port'))
    : flags.has('vite-port')
      ? vitePort + 1
      : freeServer
  if (!Number.isInteger(vitePort) || !Number.isInteger(serverPort)) {
    log.error('--vite-port / --server-port must be integers.')
    process.exit(1)
  }
  return { vitePort, serverPort }
}

/** Copies the template, applies module/manifest/webhook substitutions, optionally installs + inits git. */
const scaffold = async (answers: Answers): Promise<void> => {
  // `routes` and `webhookHandlers` are NOT in vars — they're replaced by
  // targeted edits after copyTree's mustache pass so the placeholders survive
  // past `substitute()`.
  const depVersion = internalDepVersion()
  const baseVars = {
    id: answers.id,
    name: answers.name,
    tolgeeUrl: answers.tolgeeUrl,
    sdkVersion: depVersion,
    devVersion: depVersion,
  }

  const s = spinner()
  s.start('Copying template')
  await copyTree({ src: join(TEMPLATE_ROOT, 'base'), dst: answers.targetDir, vars: baseVars })

  const iframeModules = answers.selectedModules.filter(
    (k) => MODULES.find((m) => m.key === k)?.hasIframe ?? false
  )
  for (const moduleKey of iframeModules) {
    await copyTree({
      src: join(TEMPLATE_ROOT, 'modules', moduleKey),
      dst: answers.targetDir,
      vars: baseVars,
    })
  }

  const mainTsxPath = join(answers.targetDir, 'src/main.tsx')
  const routesBody = iframeModules
    .map((k) => {
      const r = MODULE_ENTRY_PATHS[k]
      return `  '${r.route}': () => import('${r.importPath}'),`
    })
    .join('\n')
  await replaceInFile(mainTsxPath, '  // {{routes}}', routesBody)

  const webhookPath = join(answers.targetDir, 'server/routes/webhook.ts')
  if (answers.webhookEvents.length === 0) {
    // No events selected: drop the now-unused `onWebhook` import (the project
    // has noUnusedLocals) and keep `payload` referenced.
    await replaceInFile(
      webhookPath,
      "import { onWebhook, receiveWebhook } from '@tolgee/apps-sdk/server'",
      "import { receiveWebhook } from '@tolgee/apps-sdk/server'"
    )
    await replaceInFile(
      webhookPath,
      '  // {{webhookHandlers}}',
      '  // No webhook events selected — add `onWebhook(payload, ...)` calls here.\n  void payload'
    )
  } else {
    const handlersBody = answers.webhookEvents
      .map(
        (event) =>
          `  onWebhook(payload, '${event}', (typed) => {\n    console.log('[webhook] ${event}', typed.activityData?.timestamp)\n  })`
      )
      .join('\n')
    await replaceInFile(webhookPath, '  // {{webhookHandlers}}', handlersBody)
  }

  const manifest = buildManifest({
    id: answers.id,
    name: answers.name,
    selectedModules: answers.selectedModules,
    scopes: answers.scopes,
    webhookEvents: answers.webhookEvents,
  })
  const manifestPath = join(answers.targetDir, 'server/manifest.template.json')
  await mkdir(dirname(manifestPath), { recursive: true })
  await writeFile(manifestPath, manifest, 'utf8')

  // Write a ready-to-run .env.local. The webhook secret is NOT written here —
  // `tolgee-app register` stores it in .tolgee-dev/install.json and the server
  // reads it from there.
  const envLocal =
    `# Local dev config written by create-tolgee-app. Gitignored.\n` +
    `TOLGEE_URL=${answers.tolgeeUrl}\n` +
    `VITE_PORT=${answers.vitePort}\n` +
    `SERVER_PORT=${answers.serverPort}\n`
  await writeFile(join(answers.targetDir, '.env.local'), envLocal, 'utf8')

  s.stop(`Project scaffolded (dev ports ${answers.vitePort}/${answers.serverPort})`)

  const pm = detectPackageManager()
  if (answers.installDeps) {
    const install = spinner()
    install.start(`Installing dependencies with ${pm}`)
    const result = spawnSync(pm, ['install'], { cwd: answers.targetDir, stdio: 'inherit' })
    if (result.status !== 0) {
      install.stop(`${pm} install failed`)
      process.exit(result.status ?? 1)
    }
    install.stop('Dependencies installed')
  }

  if (answers.gitInit) {
    spawnSync('git', ['init', '-q'], { cwd: answers.targetDir })
    spawnSync('git', ['add', '.'], { cwd: answers.targetDir })
    spawnSync('git', ['commit', '-q', '-m', 'chore: scaffold with create-tolgee-app'], {
      cwd: answers.targetDir,
      stdio: 'ignore',
    })
  }
}

const replaceInFile = async (
  path: string,
  needle: string,
  replacement: string
): Promise<void> => {
  const current = await readFile(path, 'utf8')
  if (!current.includes(needle)) return
  await writeFile(path, current.replace(needle, replacement), 'utf8')
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
