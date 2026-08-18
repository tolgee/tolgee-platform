import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, join, resolve } from 'node:path'
import { log, note, outro, spinner } from '@clack/prompts'
import pc from 'picocolors'
import { copyTree } from './copy'
import { buildManifest } from './manifest'
import { PACKAGE_ROOT, TEMPLATE_ROOT } from './paths'
import {
  DEFAULT_SERVER_PORT,
  DEFAULT_TOLGEE_URL,
  DEFAULT_VITE_PORT,
  SDK_MODES,
  type SdkMode,
} from './registry'
import { findLocalSdk, resolveSdk } from './sdk'
import {
  detectPackageManager,
  normalizeUrl,
  packageManagerCommand,
  runWizard,
  toTitle,
  validateId,
  type Answers,
} from './wizard'

const main = async (): Promise<void> => {
  const answers = await resolveAnswers(process.argv.slice(2))

  if (existsSync(answers.targetDir)) {
    log.error(`Target ${pc.bold(answers.targetDir)} already exists.`)
    process.exit(1)
  }

  await scaffold(answers)
  note(nextSteps(answers).join('\n'), 'Next steps')
  outro(pc.cyan(`${answers.name} is ready.`))
}

/**
 * Resolves answers either from the interactive wizard or, when `--yes`/`-y` is
 * passed, from CLI flags + defaults (headless mode for CI and scripted scaffolds).
 *
 *     create-tolgee-app my-app --yes --tolgee-url=http://localhost:8718
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

  const sdkMode = (flags.get('sdk') ?? 'auto') as SdkMode
  if (!SDK_MODES.includes(sdkMode)) {
    log.error(`--sdk must be one of: ${SDK_MODES.join(', ')}`)
    process.exit(1)
  }
  if (sdkMode === 'local' && !findLocalSdk(PACKAGE_ROOT)) {
    log.error(
      '--sdk=local needs the Tolgee sources: run the generator from a Tolgee ' +
        'checkout, or drop the flag to fall back to the published SDK.'
    )
    process.exit(1)
  }

  if (!nonInteractive) {
    return runWizard(positional, sdkMode)
  }

  const id = positional ?? flags.get('id')
  const idError = validateId(id)
  if (idError) {
    log.error(`${idError} Try \`create-tolgee-app my-app --yes\`.`)
    process.exit(1)
  }

  const targetDir = resolve(process.cwd(), id as string)

  return {
    id: id as string,
    name: flags.get('name') ?? toTitle(id as string),
    targetDir,
    tolgeeUrl: normalizeUrl(flags.get('tolgee-url') ?? DEFAULT_TOLGEE_URL),
    vitePort: DEFAULT_VITE_PORT,
    serverPort: DEFAULT_SERVER_PORT,
    sdk: resolveSdk({ mode: sdkMode, targetDir, packageRoot: PACKAGE_ROOT }),
    installDeps: flags.has('install'),
    gitInit: flags.has('git'),
  }
}

/** Copies the template, writes the manifest + `.env.local`, optionally installs + inits git. */
const scaffold = async (answers: Answers): Promise<void> => {
  const s = spinner()
  s.start('Copying template')

  await copyTree({
    src: TEMPLATE_ROOT,
    dst: answers.targetDir,
    vars: {
      id: answers.id,
      name: answers.name,
      tolgeeUrl: answers.tolgeeUrl,
      vitePort: String(answers.vitePort),
      serverPort: String(answers.serverPort),
      manifestUrl: manifestUrl(answers),
      sdkSpec: answers.sdk.spec,
    },
  })

  const manifestPath = join(answers.targetDir, 'server/manifest.template.json')
  await mkdir(dirname(manifestPath), { recursive: true })
  await writeFile(manifestPath, buildManifest(answers), 'utf8')
  await writeFile(join(answers.targetDir, '.env.local'), envLocal(answers), 'utf8')

  s.stop(`Project scaffolded in ${pc.bold(answers.id)}`)

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
    spawnSync('git', ['commit', '-q', '-m', 'chore: scaffold with create-tolgee-app'], {
      cwd: answers.targetDir,
      stdio: 'ignore',
    })
  }
}

/** URL Tolgee fetches the manifest from — the Express server, not Vite. */
const manifestUrl = (answers: Answers): string =>
  `http://localhost:${answers.serverPort}/manifest.json`

const isLocalUrl = (value: string): boolean => {
  try {
    const { hostname } = new URL(value)
    return ['localhost', '127.0.0.1', '::1', '[::1]'].includes(hostname)
  } catch {
    return false
  }
}

const envLocal = (answers: Answers): string => {
  const lines = [
    '# Local dev config written by create-tolgee-app. Gitignored.',
    `TOLGEE_URL=${answers.tolgeeUrl}`,
    `VITE_PORT=${answers.vitePort}`,
    `SERVER_PORT=${answers.serverPort}`,
    '',
    "# Origin of Tolgee's web app — the page that frames this app. Set it when",
    '# it differs from TOLGEE_URL (a dev Tolgee serves the web app and the API',
    '# on different ports) or the iframe stays on "Waiting for Tolgee…".',
    '# TOLGEE_FRONTEND_URL=http://localhost:3718',
    '',
    '# Uncomment to keep `npm run dev` from opening a Cloudflare tunnel when',
    '# TOLGEE_URL is not localhost. See .env.example.',
    '# TOLGEE_DEV_TUNNEL=none',
  ]
  lines.push(
    '',
    '# Tolgee-internal: set the server registration secret (from the Tolgee',
    '# administrator) and the app registers itself on boot — into',
    "# TOLGEE_APP_ORGANIZATION, or the server's initial organization when unset.",
    '# Without it, register the app in Tolgee under Organization → Apps.',
    '# TOLGEE_APP_REGISTRATION_TOKEN=',
    '# TOLGEE_APP_ORGANIZATION='
  )
  return lines.join('\n') + '\n'
}

const nextSteps = (answers: Answers): string[] => {
  const pm = detectPackageManager()
  const steps: string[] = []

  steps.push(`SDK: ${answers.sdk.summary}`)
  if (answers.sdk.buildRequired && answers.sdk.sdkDir) {
    steps.push(
      'That SDK has no dist/ yet — build it first or the app will not typecheck:',
      `  (cd ${answers.sdk.sdkDir} && ${packageManagerCommand(pm, 'run', 'build')})`
    )
  }
  if (answers.sdk.source === 'published') {
    steps.push(
      'The published SDK can trail this generator — pass --sdk=local from a',
      'Tolgee checkout to build against the SDK sources instead.'
    )
  }
  steps.push('')

  steps.push(
    `cd ${answers.id}`,
    ...(answers.installDeps ? [] : [packageManagerCommand(pm, 'install')]),
    packageManagerCommand(pm, 'run', 'dev'),
    ''
  )

  if (isLocalUrl(answers.tolgeeUrl)) {
    steps.push(
      `In ${answers.tolgeeUrl}, go to Organization → Apps → add an app with`,
      `manifest URL ${manifestUrl(answers)}`,
      ''
    )
  } else {
    steps.push(
      `${answers.tolgeeUrl} cannot reach localhost, so ${packageManagerCommand(pm, 'run', 'dev')} opens a`,
      'Cloudflare tunnel and prints a public manifest URL. Add an app with that',
      'URL in Organization → Apps.',
      ''
    )
  }

  steps.push(
    `Then enable "${answers.name}" for a project: Project → Settings → Apps.`
  )
  return steps
}

main().catch((err: unknown) => {
  // These are the generator's own "you cannot get there from here" messages —
  // a stack trace would only bury them.
  log.error(err instanceof Error ? err.message : String(err))
  process.exit(1)
})
