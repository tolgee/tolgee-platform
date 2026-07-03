#!/usr/bin/env node
/*
 * Fails if any translation key used in the code does not exist in the Tolgee
 * project. Run *after* `tolgee pull` (npm run load-translations): the pulled
 * `src/i18n/*.json` files are the source of truth for which keys exist.
 *
 * This replaces the old `tsc --project tsconfig.prod.json` check, whose strict
 * `TranslationKey` override silently stopped applying after the @tolgee v7
 * upgrade: its `declare module '@tolgee/core/lib/types'` no longer matched
 * where the type is defined (`@tolgee/core/lib/types/general`), and a `type`
 * alias can't be overridden by module augmentation anyway. `TranslationKey`
 * therefore fell back to `string`, so `t()` / `<T keyName>` accepted any key
 * and no missing key was ever caught.
 *
 * This check is extractor-based and deterministic. It refuses to pass when its
 * own inputs look broken (see the guards below), so a future regression fails
 * loudly instead of silently passing.
 */
import { execFileSync } from 'node:child_process';
import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const webappDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const i18nDir = resolve(webappDir, 'src/i18n');

const fail = (msg) => {
  console.error(`\n❌ ${msg}\n`);
  process.exit(1);
};

// 1. Known keys = union of every key across the pulled base + translation files.
if (!existsSync(i18nDir)) {
  fail(`${i18nDir} not found. Run \`npm run load-translations\` (tolgee pull) first.`);
}
const jsonFiles = readdirSync(i18nDir).filter((f) => f.endsWith('.json'));
const known = new Set();
// Pulled files mix flat dotted keys ("a.b" as one key) with nested objects
// (Tolgee's `.` structure delimiter). Flatten so both yield the dotted key the
// extractor reports: recurse into objects, treat every string leaf as a key.
const collect = (obj, prefix) => {
  for (const [k, v] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${k}` : k;
    if (v && typeof v === 'object' && !Array.isArray(v)) collect(v, path);
    else known.add(path);
  }
};
for (const f of jsonFiles) {
  collect(JSON.parse(readFileSync(resolve(i18nDir, f), 'utf8')), '');
}
// Guard: an empty/absent pull must fail loudly rather than pass everything.
if (known.size === 0) {
  fail('No translation keys found in src/i18n/*.json — the pull likely failed. Refusing to pass.');
}

// 2. Keys actually used in the code (AST extraction — same extractor as `tolgee extract`).
let output;
try {
  output = execFileSync('npx', ['tolgee', 'extract', 'print'], {
    cwd: webappDir,
    encoding: 'utf8',
    maxBuffer: 128 * 1024 * 1024,
  });
} catch (e) {
  fail(`\`tolgee extract print\` failed:\n${e.stdout || ''}${e.stderr || ''}`);
}

const missing = [];
const usedKeys = new Set();
let currentFile = null;
for (const line of output.split('\n')) {
  const fileMatch = line.match(/^\d+ keys? found in (.+):$/);
  if (fileMatch) {
    currentFile = fileMatch[1];
    continue;
  }
  const keyMatch = line.match(/^\tline (\d+): (.+)$/);
  if (keyMatch) {
    const [, lineNo, key] = keyMatch;
    usedKeys.add(key);
    if (!known.has(key)) missing.push({ file: currentFile, line: lineNo, key });
  }
}
// Guard: if the extractor suddenly finds nothing, its output format probably
// changed — don't let that masquerade as "no missing keys".
if (usedKeys.size === 0) {
  fail('The extractor reported 0 used keys — extraction or its output format is broken. Refusing to pass.');
}

if (missing.length) {
  const uniqueCount = new Set(missing.map((m) => m.key)).size;
  console.error(
    `\n❌ ${uniqueCount} translation key(s) are used in the code but do not exist in the Tolgee project:\n`
  );
  for (const { file, line, key } of missing) {
    console.error(`   ${file}:${line}  →  ${key}`);
  }
  console.error(
    '\nCreate these keys in the Tolgee project (or fix the key names), then re-pull. ' +
      'An inline defaultValue does NOT make a key exist in Tolgee.\n'
  );
  process.exit(1);
}

console.log(
  `✓ All extracted keys exist in Tolgee (${usedKeys.size} used, ${known.size} known across ${jsonFiles.length} language file(s)).`
);
