// if src/ee exists copy the webapp/src/eePlugin.ee.tsx to webapp/src/eePlugin.local.tsx
// else copy the webapp/src/eePlugin.oss.tsx to webapp/src/eePlugin.local.tsx

import { existsSync, symlinkSync, unlinkSync } from 'fs';

import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const srcDir = path.join(__dirname, '../src/ee');
const eePluginEe = path.join(__dirname, '../eePlugin/eePlugin.ee.tsx');
const eePluginOss = path.join(__dirname, '../eePlugin/eePlugin.oss.tsx');
const eePluginLocal = path.join(__dirname, '../src/eePlugin.local.tsx');

if (existsSync(eePluginLocal)) {
  unlinkSync(eePluginLocal);
}

if (existsSync(srcDir)) {
  symlinkSync(eePluginEe, eePluginLocal);
} else {
  symlinkSync(eePluginOss, eePluginLocal);
}
