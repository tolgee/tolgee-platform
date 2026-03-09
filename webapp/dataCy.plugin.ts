import { Plugin } from 'vite';
import { writeFile } from 'fs/promises';
import { resolve } from 'node:path';

import {
  formatOutput,
  getFiles,
  processFiles,
  getSortedItems,
} from './dataCy.core.mjs';

const SRC_PATH = resolve('./src');
const OUTPUT_PATH = resolve('../e2e/cypress/support/dataCyType.d.ts');

export function extractDataCy(): Plugin {
  // Incremental state for watchChange — maps a file path to its dataCy values
  const fileItems: Record<string, string[]> = {};

  return {
    name: 'extract-data-cy',
    async buildStart() {
      const files = await getFiles(SRC_PATH);
      await processFiles(files, fileItems);
      const sortedItems = getSortedItems(fileItems);
      await writeFile(OUTPUT_PATH, formatOutput(sortedItems));
    },
    async watchChange(id) {
      await processFiles([id], fileItems);
      const sortedItems = getSortedItems(fileItems);
      await writeFile(OUTPUT_PATH, formatOutput(sortedItems));
    },
  };
}
