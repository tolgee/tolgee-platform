import { mkdir, readdir, readFile, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'

type Vars = Record<string, string>

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
      await mkdir(dirname(dstPath), { recursive: true })
      if (isProbablyBinary(entry.name)) {
        await writeFile(dstPath, await readFile(srcPath))
      } else {
        const text = await readFile(srcPath, 'utf8')
        await writeFile(dstPath, substitute(text, opts.vars), 'utf8')
      }
    }
  }
}

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
