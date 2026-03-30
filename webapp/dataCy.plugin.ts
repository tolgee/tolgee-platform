import { Plugin } from 'vite';
import { writeFile } from 'fs/promises';
import { resolve } from 'node:path';
import { existsSync } from 'node:fs';

import {
  formatOutput,
  getFiles,
  processFiles,
  getSortedItems,
} from './dataCy.core.mjs';

const SRC_PATH = resolve('./src');
const OUTPUT_PATH = resolve('../e2e/cypress/support/dataCyType.d.ts');

const BILLING_SRC_PATH = resolve('../../billing/frontend/billing');
const BILLING_OUTPUT_PATH = resolve(
  '../../billing/e2e/cypress/support/dataCyType.d.ts'
);
const hasBilling =
  existsSync(BILLING_SRC_PATH) &&
  existsSync(resolve(BILLING_OUTPUT_PATH, '..'));

export function extractDataCy(): Plugin {
  const fileItems: Record<string, string[]> = {};
  const billingFileItems: Record<string, string[]> = {};

  async function writePlatform() {
    const allItems = hasBilling
      ? { ...fileItems, ...billingFileItems }
      : fileItems;
    const sortedItems = getSortedItems(allItems);
    await writeFile(OUTPUT_PATH, formatOutput(sortedItems));
  }

  async function writeBilling() {
    if (!hasBilling) return;
    const sortedItems = getSortedItems(billingFileItems);
    await writeFile(BILLING_OUTPUT_PATH, formatOutput(sortedItems));
  }

  return {
    name: 'extract-data-cy',
    async buildStart() {
      const files = await getFiles(SRC_PATH);
      await processFiles(files, fileItems);
      await writePlatform();

      if (hasBilling) {
        const billingFiles = await getFiles(BILLING_SRC_PATH);
        await processFiles(billingFiles, billingFileItems);
        await writeBilling();
      }
    },
    async watchChange(id) {
      if (hasBilling && id.startsWith(BILLING_SRC_PATH)) {
        await processFiles([id], billingFileItems);
        await writeBilling();
        await writePlatform();
      } else {
        await processFiles([id], fileItems);
        await writePlatform();
      }
    },
  };
}
