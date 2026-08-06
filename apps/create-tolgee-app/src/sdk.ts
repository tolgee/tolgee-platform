import { existsSync, readFileSync } from 'node:fs'
import { isAbsolute, join, relative, resolve, sep } from 'node:path'
import {
  PUBLISHED_SDK_RELEASED,
  PUBLISHED_SDK_VERSION,
  SDK_PACKAGE_NAME,
  type SdkMode,
} from './registry'

export type SdkSource =
  /** Generated inside this repo — npm workspaces links the SDK. */
  | 'workspace'
  /** `file:` dependency on the SDK directory of this checkout. */
  | 'local'
  /** Version range from the npm registry. */
  | 'published'

export type SdkResolution = {
  /** Value written as the generated app's `@tolgee/apps-sdk` dependency. */
  spec: string
  source: SdkSource
  /** Absolute path of the SDK in this checkout; null when none was found. */
  sdkDir: string | null
  /** A local SDK is used but its `dist/` is missing, so the app cannot typecheck yet. */
  buildRequired: boolean
  /** One-liner shown in the wizard summary and in the next steps. */
  summary: string
}

export type ResolveSdkInput = {
  mode: SdkMode
  /** Absolute path the app is generated into. */
  targetDir: string
  /** Root of the create-tolgee-app package (see `PACKAGE_ROOT`). */
  packageRoot: string
}

/**
 * Decides which `@tolgee/apps-sdk` the generated app depends on.
 *
 * `auto` prefers the SDK of this checkout, because the published package trails
 * it by whole APIs. Inside the workspace root a range is enough — npm links the
 * local package — everywhere else the dependency has to name the directory
 * outright, or npm resolves it from the registry instead.
 *
 * Throws in `local` mode when this CLI has no SDK sources next to it, and
 * whenever the registry is the only option left while no released SDK carries
 * the API the template uses (see `PUBLISHED_SDK_RELEASED`).
 */
export const resolveSdk = (input: ResolveSdkInput): SdkResolution => {
  const sdkDir = findLocalSdk(input.packageRoot)

  if (input.mode === 'published' || (input.mode === 'auto' && !sdkDir)) {
    if (!PUBLISHED_SDK_RELEASED) throw noPublishedSdkError(input.mode)
    const version = (sdkDir && localSdkVersion(sdkDir)) ?? PUBLISHED_SDK_VERSION
    return {
      spec: version,
      source: 'published',
      sdkDir,
      buildRequired: false,
      summary: `published ${SDK_PACKAGE_NAME}@${version}`,
    }
  }

  if (!sdkDir) {
    throw new Error(
      `--sdk=local needs the SDK sources next to this CLI ` +
        `(${resolve(input.packageRoot, '..', 'tolgee-apps-sdk')}), and they are not there. ` +
        `Run the generator from a Tolgee checkout, or use --sdk=published.`
    )
  }

  const workspaceRoot = workspaceRootOf(sdkDir)
  const buildRequired = !isBuilt(sdkDir)

  if (isWorkspaceTarget(input.targetDir, workspaceRoot)) {
    return {
      spec: '*',
      source: 'workspace',
      sdkDir,
      buildRequired,
      summary: `local ${SDK_PACKAGE_NAME} via npm workspaces (${relative(workspaceRoot, sdkDir)})`,
    }
  }

  return {
    spec: `file:${sdkDir}`,
    source: 'local',
    sdkDir,
    buildRequired,
    summary: `local ${SDK_PACKAGE_NAME} (file:${sdkDir})`,
  }
}

/**
 * Locates the SDK sources relative to this CLI: both packages live side by side
 * under `apps/`. Returns null when the CLI runs from a published install, where
 * only the registry copy exists.
 */
export const findLocalSdk = (packageRoot: string): string | null => {
  const dir = resolve(packageRoot, '..', 'tolgee-apps-sdk')
  const manifest = join(dir, 'package.json')
  if (!existsSync(manifest)) return null
  try {
    const parsed = JSON.parse(readFileSync(manifest, 'utf8')) as {
      name?: unknown
    }
    return parsed.name === SDK_PACKAGE_NAME ? dir : null
  } catch {
    return null
  }
}

/**
 * Whether the generated app would become an npm workspace of the root the SDK
 * lives in, which is the one case where a plain version range still resolves to
 * the local SDK.
 */
export const isWorkspaceTarget = (
  targetDir: string,
  workspaceRoot: string
): boolean => {
  const rel = relative(workspaceRoot, targetDir)
  if (rel.length === 0 || rel.startsWith('..') || isAbsolute(rel)) return false
  const posixRel = rel.split(sep).join('/')
  return workspaceGlobs(workspaceRoot).some((glob) =>
    globToRegExp(glob).test(posixRel)
  )
}

const noPublishedSdkError = (mode: SdkMode): Error =>
  new Error(
    `No published ${SDK_PACKAGE_NAME} supports this generator yet: every release on npm ` +
      `predates selfRegisterApp, applyTolgeeTheme and mountTolgeeLifecycle, which the ` +
      `generated app imports, so ` +
      `the scaffolded project would not typecheck.\n` +
      `Run the generator from a Tolgee checkout instead — it then builds against the SDK ` +
      `sources next to it` +
      (mode === 'published' ? ' (drop --sdk=published)' : '') +
      `.\nWhen ${SDK_PACKAGE_NAME}@${PUBLISHED_SDK_VERSION} is on npm, flip ` +
      `PUBLISHED_SDK_RELEASED to true in src/registry.ts.`
  )

const localSdkVersion = (sdkDir: string): string | null => {
  try {
    const parsed = JSON.parse(
      readFileSync(join(sdkDir, 'package.json'), 'utf8')
    ) as { version?: unknown }
    return typeof parsed.version === 'string' ? parsed.version : null
  } catch {
    return null
  }
}

/** The SDK sits at `<apps root>/tolgee-apps-sdk`, and `apps/` is the workspace root. */
const workspaceRootOf = (sdkDir: string): string => resolve(sdkDir, '..')

const workspaceGlobs = (workspaceRoot: string): string[] => {
  try {
    const parsed = JSON.parse(
      readFileSync(join(workspaceRoot, 'package.json'), 'utf8')
    ) as { workspaces?: unknown }
    const raw = Array.isArray(parsed.workspaces)
      ? parsed.workspaces
      : ((parsed.workspaces as { packages?: unknown } | undefined)?.packages ??
        [])
    return Array.isArray(raw) ? raw.filter((g) => typeof g === 'string') : []
  } catch {
    return []
  }
}

const globToRegExp = (glob: string): RegExp => {
  const escaped = glob.replace(/[.+^${}()|[\]\\]/g, '\\$&')
  const body = escaped.replace(/\*+/g, (stars) =>
    stars.length > 1 ? '.*' : '[^/]*'
  )
  return new RegExp(`^${body}/?$`)
}

const isBuilt = (sdkDir: string): boolean =>
  existsSync(join(sdkDir, 'dist', 'index.js'))
