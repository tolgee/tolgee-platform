import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { log, note, outro, spinner } from '@clack/prompts'
import pc from 'picocolors'
import { copyTree, copyFiles } from './copy'
import { buildManifest } from './manifest'
import { MODULES, type ModuleKey } from './registry'
import { detectPackageManager, packageManagerCommand, runWizard } from './wizard'

const __dirname = dirname(fileURLToPath(import.meta.url))

/**
 * tsup bundles a single dist/index.js next to the project root. The
 * template/ folder ships under the package root, so we walk up one
 * level from dist/.
 */
const PACKAGE_ROOT = resolve(__dirname, '..')
const TEMPLATE_ROOT = join(PACKAGE_ROOT, 'template')

const MODULE_ENTRY_PATHS: Record<ModuleKey, { route: string; importPath: string }> = {
  'project-dashboard-page': {
    route: '/dashboard',
    importPath: './modules/dashboard',
  },
  'translation-tools-panel': {
    route: '/tools-panel',
    importPath: './modules/toolsPanel',
  },
  'key-edit-tab': {
    route: '/key-edit-tab',
    importPath: './modules/keyEditTab',
  },
  modal: {
    route: '/modal',
    importPath: './modules/modal',
  },
  'key-action': { route: '', importPath: '' },
  'translation-action': { route: '', importPath: '' },
  'bulk-action': { route: '', importPath: '' },
  'translations-toolbar-action': { route: '', importPath: '' },
  'project-menu-action': { route: '', importPath: '' },
  shortcut: { route: '', importPath: '' },
}

const main = async (): Promise<void> => {
  const initialName = process.argv[2]
  const answers = await runWizard(initialName)

  // `routes` and `webhookHandlers` are NOT in vars — they're replaced
  // by targeted edits after copyTree's mustache pass so the placeholders
  // survive past `substitute()`.
  const baseVars = {
    id: answers.id,
    name: answers.name,
    tolgeeUrl: answers.tolgeeUrl,
  }

  if (existsSync(answers.targetDir)) {
    log.error(`Target ${pc.bold(answers.targetDir)} already exists.`)
    process.exit(1)
  }

  const s = spinner()
  s.start('Copying template')
  await copyTree({
    src: join(TEMPLATE_ROOT, 'base'),
    dst: answers.targetDir,
    vars: baseVars,
  })

  const iframeModules = answers.selectedModules.filter(
    (k) => MODULES.find((m) => m.key === k)?.hasIframe ?? false
  )
  for (const moduleKey of iframeModules) {
    const moduleSrc = join(TEMPLATE_ROOT, 'modules', moduleKey)
    await copyTree({
      src: moduleSrc,
      dst: answers.targetDir,
      vars: baseVars,
    })
  }

  // Rewrite src/main.tsx with the populated routes map. The needle
  // includes the leading indent so the replacement controls its own.
  const mainTsxPath = join(answers.targetDir, 'src/main.tsx')
  const routesBody = iframeModules
    .map((k) => {
      const r = MODULE_ENTRY_PATHS[k]
      return `  '${r.route}': () => import('${r.importPath}'),`
    })
    .join('\n')
  await replaceInFile(mainTsxPath, '  // {{routes}}', routesBody)

  // Webhook handler stubs in server/routes/webhook.ts.
  const webhookPath = join(answers.targetDir, 'server/routes/webhook.ts')
  const handlersBody =
    answers.webhookEvents.length === 0
      ? '  // No webhook events selected — add `onWebhook(payload, ...)` calls here as needed.'
      : answers.webhookEvents
          .map(
            (event) =>
              `  onWebhook(payload, '${event}', (typed) => {\n    console.log('[webhook] ${event}', typed.activityData?.timestamp)\n  })`
          )
          .join('\n')
  await replaceInFile(
    webhookPath,
    '  // {{webhookHandlers}}',
    handlersBody
  )

  // Manifest template.
  const manifest = buildManifest({
    id: answers.id,
    name: answers.name,
    selectedModules: answers.selectedModules,
    webhookEvents: answers.webhookEvents,
  })
  const manifestPath = join(
    answers.targetDir,
    'server/manifest.template.json'
  )
  await mkdir(dirname(manifestPath), { recursive: true })
  await writeFile(manifestPath, manifest, 'utf8')

  s.stop('Project scaffolded')
  void copyFiles // referenced for future expansion

  const pm = detectPackageManager()
  if (answers.installDeps) {
    const install = spinner()
    install.start(`Installing dependencies with ${pm}`)
    const result = spawnSync(pm, ['install'], {
      cwd: answers.targetDir,
      stdio: 'inherit',
    })
    if (result.status !== 0) {
      install.stop(`${pm} install failed`)
      process.exit(result.status ?? 1)
    }
    install.stop('Dependencies installed')
  }

  if (answers.gitInit) {
    spawnSync('git', ['init', '-q'], { cwd: answers.targetDir })
    spawnSync('git', ['add', '.'], { cwd: answers.targetDir })
    spawnSync(
      'git',
      ['commit', '-q', '-m', 'chore: scaffold with create-tolgee-app'],
      { cwd: answers.targetDir, stdio: 'ignore' }
    )
  }

  const next: string[] = [
    `cd ${answers.id}`,
    answers.installDeps ? null : packageManagerCommand(pm, 'install'),
    packageManagerCommand(pm, 'run', 'register'),
    packageManagerCommand(pm, 'run', 'dev'),
  ].filter((s): s is string => s !== null)

  note(next.join('\n'), 'Next steps')
  outro(pc.cyan(`${answers.name} is ready.`))
}

const replaceInFile = async (
  path: string,
  needle: string,
  replacement: string
): Promise<void> => {
  const { readFile, writeFile } = await import('node:fs/promises')
  const current = await readFile(path, 'utf8')
  if (!current.includes(needle)) return
  await writeFile(path, current.replace(needle, replacement), 'utf8')
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
