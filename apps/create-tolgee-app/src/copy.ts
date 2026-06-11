import { mkdir, readdir, readFile, writeFile } from 'node:fs/promises'
import { dirname, join, relative } from 'node:path'

type Vars = Record<string, string>

const isProbablyBinary = (filename: string): boolean => {
  return /\.(png|jpg|jpeg|gif|svg|ico|webp|woff2?|ttf|otf|eot|mp4|webm)$/i.test(
    filename
  )
}

/**
 * Mustache-style replacement: `{{name}}` → vars.name. Only single-word
 * keys are substituted; missing keys leave the placeholder intact (so
 * literal `{{` appearances in user-facing copy survive).
 */
const substitute = (text: string, vars: Vars): string => {
  return text.replace(/\{\{(\w+)\}\}/g, (match, key: string) => {
    return key in vars ? vars[key]! : match
  })
}

/**
 * Renames template-only file conventions to their real names:
 *   `_package.json` → `package.json` (avoids npm seeing the template as a real package)
 *   `_X`            → `.X`           (lets the template ship dotfiles)
 */
const targetFilename = (name: string): string => {
  if (name === '_package.json') return 'package.json'
  return name.startsWith('_') ? '.' + name.slice(1) : name
}

export type CopyOptions = {
  /** Source directory inside the template. */
  src: string
  /** Destination directory for the generated project. */
  dst: string
  /** Variables substituted into text files. */
  vars: Vars
}

/**
 * Recursively copies `src` → `dst`, substituting `{{var}}` placeholders
 * in text files and renaming `_*` files to `.*`.
 */
export async function copyTree(opts: CopyOptions): Promise<void> {
  const entries = await readdir(opts.src, { withFileTypes: true })
  await mkdir(opts.dst, { recursive: true })
  for (const entry of entries) {
    const srcPath = join(opts.src, entry.name)
    const dstPath = join(opts.dst, targetFilename(entry.name))
    if (entry.isDirectory()) {
      await copyTree({ ...opts, src: srcPath, dst: dstPath })
    } else if (entry.isFile()) {
      if (isProbablyBinary(entry.name)) {
        const buf = await readFile(srcPath)
        await mkdir(dirname(dstPath), { recursive: true })
        await writeFile(dstPath, buf)
      } else {
        const text = await readFile(srcPath, 'utf8')
        await mkdir(dirname(dstPath), { recursive: true })
        await writeFile(dstPath, substitute(text, opts.vars), 'utf8')
      }
    }
  }
}

/**
 * Like {@link copyTree} but takes a list of relative source files.
 * Useful when only some files from a directory should be copied.
 */
export async function copyFiles(
  baseSrc: string,
  baseDst: string,
  relativePaths: string[],
  vars: Vars
): Promise<void> {
  for (const relPath of relativePaths) {
    const segments = relPath.split('/').map((s) => targetFilename(s))
    const srcPath = join(baseSrc, relPath)
    const dstPath = join(baseDst, segments.join('/'))
    const text = await readFile(srcPath, 'utf8')
    await mkdir(dirname(dstPath), { recursive: true })
    await writeFile(dstPath, substitute(text, vars), 'utf8')
  }
}

/** For diagnostics only — relative path from a template root. */
export const fromTemplate = (templateRoot: string, abs: string): string => {
  return relative(templateRoot, abs)
}
