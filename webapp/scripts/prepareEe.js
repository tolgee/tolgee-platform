// if src/ee exists copy the webapp/src/eeModule.ee.tsx to webapp/src/eeModule.local.tsx
// else copy the webapp/src/eeModule.oss.tsx to webapp/src/eeModule.local.tsx

import { existsSync, lstatSync, symlinkSync, unlinkSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const srcDir = join(__dirname, '../src/ee');
const eeModuleEe = join(__dirname, '../src/eeSetup/eeModule.ee.tsx');
const eeModuleOss = join(__dirname, '../src/eeSetup/eeModule.oss.tsx');
const eeModuleLocal = join(__dirname, '../src/eeSetup/eeModule.current.tsx');

// remove the symlink if it exists
if (!!lstatSync(eeModuleLocal, { throwIfNoEntry: false })) {
  unlinkSync(eeModuleLocal);
}

if (existsSync(srcDir)) {
  symlinkSync(eeModuleEe, eeModuleLocal);
} else {
  symlinkSync(eeModuleOss, eeModuleLocal);
}
