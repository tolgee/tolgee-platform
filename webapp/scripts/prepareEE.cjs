// if src/ee exists copy the webapp/src/eePlugin.ee.tsx to webapp/src/eePlugin.local.tsx
// else copy the webapp/src/eePlugin.oss.tsx to webapp/src/eePlugin.local.tsx

const { existsSync, copyFileSync } = require('fs');
const { join } = require('path');

const srcDir = join(__dirname, '../src/ee');
const eePluginEe = join(__dirname, '../src/eePlugin.ee.tsx');
const eePluginOss = join(__dirname, '../src/eePlugin.oss.tsx');
const eePluginLocal = join(__dirname, '../src/eePlugin.local.tsx');

if (existsSync(srcDir)) {
  copyFileSync(eePluginEe, eePluginLocal);
} else {
  copyFileSync(eePluginOss, eePluginLocal);
}
