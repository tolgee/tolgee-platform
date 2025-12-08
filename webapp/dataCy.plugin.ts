import { Plugin } from 'vite';
import { readdir, readFile, writeFile } from 'fs/promises';
import { resolve } from 'node:path';
import { existsSync } from 'node:fs';

const SRC_PATH = resolve('./src');

export function extractDataCy(): Plugin {
  const fileItems: Record<string, string[]> = {};

  async function generate(files: string[]) {
    await processFiles(files);
    const sortedItems = getSortedItems();
    const fileContent = await generateFileContent(sortedItems);
    await writeToFile(fileContent);
  }

  async function processFiles(files: string[]) {
    for (const file of files) {
      await processFile(file);
    }
  }

  async function writeToFile(fileContent: string) {
    await writeFile(
      resolve(`../e2e/cypress/support/dataCyType.d.ts`),
      fileContent
    );
  }

  async function generateFileContent(sortedItems) {
    let fileContent = 'declare namespace DataCy {\n';
    fileContent +=
      '    export type Value = \n        ' +
      sortedItems.map((i) => `"${i}"`).join(' |\n        ') +
      '\n}';
    return fileContent;
  }

  function getSortedItems() {
    const items = Object.values(fileItems).reduce(
      (acc, curr) => [...acc, ...curr],
      []
    );
    const itemsSet = new Set(items);
    return [...itemsSet].sort();
  }

  async function processFile(file: string) {
    if (/.*\.tsx?$/.test(file)) {
      if (!existsSync(file)) {
        fileItems[file] = [];
        return;
      }
      const content = (await readFile(file)).toString();
      const matches = content.matchAll(
        /["']?data-?[c|C]y["']?\s*[=:]\s*{?["'`]([A-Za-z0-9-_\s]+)["'`]?}?/g
      );
      fileItems[file] = [];
      for (const match of matches) {
        fileItems[file].push(match[1]);
      }
    }
  }

  async function getFiles(dir: string) {
    const dirents = await readdir(dir, { withFileTypes: true });
    const files = await Promise.all(
      dirents.map((dirent) => {
        const res = resolve(dir, dirent.name);
        return dirent.isDirectory() ? getFiles(res) : res;
      })
    );
    return Array.prototype.concat(...files);
  }

  return {
    name: 'extract-data-cy',
    async buildStart() {
      const files = await getFiles(SRC_PATH);
      await generate(files);
    },
    async watchChange(id) {
      generate([id]);
    },
  };
}
