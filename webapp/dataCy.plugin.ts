import { Plugin } from 'vite';
import { writeFile } from 'fs/promises';
import { existsSync } from 'node:fs';
import { resolve } from 'node:path';

import {
  formatOutput,
  getFiles,
  processFiles,
  getSortedItems,
} from './dataCy.core.mjs';

const SRC_PATH = resolve('./src');
const OUTPUT_PATH = resolve('../e2e/cypress/support/dataCyType.d.ts');

// The billing frontend is a separate repo built together with the webapp.
// It owns its own e2e dataCyType.d.ts (merged with the platform one via
// TypeScript interface merging), so regenerate it here as well — that way
// running the webapp keeps both files in sync.
const BILLING_SRC_PATH = resolve('../../billing/frontend/src');
const BILLING_OUTPUT_PATH = resolve(
  '../../billing/e2e/cypress/support/dataCyType.d.ts'
);
const hasBilling = existsSync(BILLING_SRC_PATH);

export function extractDataCy(): Plugin {
  const fileItems: Record<string, string[]> = {};
  const billingFileItems: Record<string, string[]> = {};

  async function writeDataCy(
    items: Record<string, string[]>,
    outputPath: string
  ) {
    await writeFile(outputPath, formatOutput(getSortedItems(items)));
  }

  return {
    name: 'extract-data-cy',
    async buildStart() {
      const files = await getFiles(SRC_PATH);
      await processFiles(files, fileItems);
      await writeDataCy(fileItems, OUTPUT_PATH);

      if (hasBilling) {
        const billingFiles = await getFiles(BILLING_SRC_PATH);
        await processFiles(billingFiles, billingFileItems);
        await writeDataCy(billingFileItems, BILLING_OUTPUT_PATH);
      }
    },
    async watchChange(id) {
      if (hasBilling && id.startsWith(BILLING_SRC_PATH)) {
        await processFiles([id], billingFileItems);
        await writeDataCy(billingFileItems, BILLING_OUTPUT_PATH);
      } else {
        await processFiles([id], fileItems);
        await writeDataCy(fileItems, OUTPUT_PATH);
      }
    },
  };
}
