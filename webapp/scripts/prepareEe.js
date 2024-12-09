// if src/ee exists copy the webapp/src/eeModule.ee.tsx to webapp/src/eeModule.local.tsx
// else copy the webapp/src/eeModule.oss.tsx to webapp/src/eeModule.local.tsx

import { existsSync, symlinkSync, unlinkSync } from 'fs';

import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const srcDir = path.join(__dirname, '../src/ee');
const eeModuleEe = path.join(__dirname, '../src/eeSetup/eeModule.ee.tsx');
const eeModuleOss = path.join(__dirname, '../src/eeSetup/eeModule.oss.tsx');
const eeModuleLocal = path.join(
  __dirname,
  '../src/eeSetup/eeModule.current.tsx'
);

if (existsSync(eeModuleLocal)) {
  unlinkSync(eeModuleLocal);
}

if (existsSync(srcDir)) {
  symlinkSync(eeModuleEe, eeModuleLocal);
} else {
  symlinkSync(eeModuleOss, eeModuleLocal);
}
