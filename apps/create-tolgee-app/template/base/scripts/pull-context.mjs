#!/usr/bin/env node
/**
 * Pulls Tolgee sources + docs into `.context/` (gitignored) so an AI assistant
 * has the SDK source, the example apps, and the documentation locally when
 * building this Tolgee App. The result is a read-only snapshot (no .git), so
 * each run replaces the previous one.
 *
 * Alpha note: Tolgee Apps is in alpha, so context is pulled from the apps
 * feature branches. Update the refs below once Apps ships to `main`.
 */
import { execSync } from 'node:child_process';
import { mkdirSync, readdirSync, rmSync } from 'node:fs';
import { join } from 'node:path';

const SOURCES = [
  {
    dir: 'tolgee-platform',
    repo: 'https://github.com/tolgee/tolgee-platform.git',
    branch: 'jancizmar/tolgee-apps-poc',
    // SDK source + example apps live under apps/.
    sparse: ['apps'],
  },
  {
    dir: 'documentation',
    repo: 'https://github.com/tolgee/documentation.git',
    branch: 'jancizmar/tolgee-apps-poc',
    sparse: ['tolgee-apps'],
  },
];

const contextDir = join(process.cwd(), '.context');
mkdirSync(contextDir, { recursive: true });

const run = (cmd, cwd) =>
  execSync(cmd, { cwd, stdio: 'inherit', shell: '/bin/sh' });

for (const src of SOURCES) {
  const dest = join(contextDir, src.dir);
  rmSync(dest, { recursive: true, force: true });
  console.log(`Pulling ${src.dir} (${src.branch})…`);
  // Shallow + sparse: only the directories we need, not the whole history/tree.
  run(
    `git clone --quiet --depth 1 --filter=blob:none --sparse --branch ${src.branch} ${src.repo} "${dest}"`
  );
  run(`git sparse-checkout set ${src.sparse.join(' ')}`, dest);
  // Drop git history — .context is a read-only snapshot, not a working clone.
  rmSync(join(dest, '.git'), { recursive: true, force: true });
  // Cone-mode sparse-checkout also materializes root-level files (heavy README
  // GIFs etc.); keep only the directories we asked for.
  for (const entry of readdirSync(dest)) {
    if (!src.sparse.includes(entry)) {
      rmSync(join(dest, entry), { recursive: true, force: true });
    }
  }
}

console.log('\n.context/ is ready (gitignored). See CLAUDE.md for what lives where.');
