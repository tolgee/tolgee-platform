import { readdir, readFile, writeFile } from 'fs/promises';
import { resolve } from 'node:path';
import { existsSync } from 'node:fs';

export async function generateAll(srcPath, outputPath) {
  const files = await getFiles(srcPath);
  const fileItems = await processFiles(files);
  const sortedItems = getSortedItems(fileItems);
  await writeFile(outputPath, formatOutput(sortedItems));
}

export async function processFiles(files, fileItems = undefined) {
  fileItems = fileItems || {};
  for (const file of files) {
    if (/.*\.tsx?$/.test(file)) {
      if (!existsSync(file)) {
        fileItems[file] = [];
        continue;
      }
      const content = (await readFile(file)).toString();
      fileItems[file] = processFile(content);
    }
  }
  return fileItems;
}

export function getSortedItems(fileItems) {
  const items = Object.values(fileItems).reduce(
    (acc, curr) => [...acc, ...curr],
    []
  );
  const itemsSet = new Set(items);
  return [...itemsSet].sort();
}

export async function getFiles(dir) {
  const dirents = await readdir(dir, { withFileTypes: true });
  const files = await Promise.all(
    dirents.map((dirent) => {
      const res = resolve(dir, dirent.name);
      return dirent.isDirectory() ? getFiles(res) : res;
    })
  );
  return Array.prototype.concat(...files);
}

export function processFile(content) {
  const matches = content.matchAll(
    /["']?data-?[cC]y["']?\s*[=:]\s*{?["'`]([A-Za-z0-9-_\s]+)["'`]?}?/g
  );
  const items = [];
  for (const match of matches) {
    items.push(match[1]);
  }
  return items;
}

export function formatOutput(sortedItems) {
  let fileContent = '// This file was generated. DO NOT edit manually.\n';
  fileContent += '// Use `npm run generate-data-cy` to update this file.\n\n';

  fileContent += 'declare namespace DataCy {\n';
  fileContent += '    interface Values {\n';
  fileContent += sortedItems
    .map((i) => `        "${i}": true;`)
    .join('\n');
  fileContent += '\n    }\n';
  fileContent += '    export type Value = keyof Values;\n';
  fileContent += '}\n';
  return fileContent;
}
