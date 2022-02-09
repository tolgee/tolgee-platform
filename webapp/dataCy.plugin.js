const { resolve } = require('path');
const { readdir, readFile, writeFile } = require('fs').promises;
const { existsSync } = require('fs');
const path = require('path');
const watch = require('glob-watcher');

const SRC_PATH = path.resolve('./src');

/**
 * Plugin generating types for Cypress data-cy items
 */
class DataCyPlugin {
  fileItems = {};

  apply(compiler) {
    compiler.hooks.afterPlugins.tap('DataCyPlugin', async () => {
      const files = await this.getFiles(SRC_PATH);
      await this.generate(files);
    });

    if (process.env.NODE_ENV === 'development') {
      compiler.hooks.afterPlugins.tap('RunDataCyWathc', () => {
        const watcher = watch([`${SRC_PATH}/**/*.tsx`, `${SRC_PATH}/**/*.ts`]);

        watcher.on('change', (path) => {
          this.generate([path]);
        });

        watcher.on('add', (path) => {
          this.generate([path]);
        });

        watcher.on('unlink', (path) => {
          this.generate([path]);
        });
      });
    }
  }

  async generate(files) {
    await this.processFiles(files);
    const sortedItems = this.getSortedItems();
    let fileContent = this.generateFileContent(sortedItems);
    await this.writeFile(fileContent);
  }

  async processFiles(files) {
    for (let file of files) {
      await this.processFile(file);
    }
  }

  async writeFile(fileContent) {
    await writeFile(
      path.resolve(`../e2e/cypress/support/dataCyType.d.ts`),
      fileContent
    );
  }

  generateFileContent(sortedItems) {
    let fileContent = 'declare namespace DataCy {\n';
    fileContent +=
      '    export type Value = \n        ' +
      sortedItems.map((i) => `"${i}"`).join(' |\n        ') +
      '\n}';
    return fileContent;
  }

  getSortedItems() {
    const items = Object.values(this.fileItems).reduce(
      (acc, curr) => [...acc, ...curr],
      []
    );
    const itemsSet = new Set(items);
    return [...itemsSet].sort();
  }

  async processFile(file) {
    if (/.*\.tsx?$/.test(file)) {
      if (!existsSync(file)) {
        this.fileItems[file] = [];
        return;
      }
      const content = (await readFile(file)).toString();
      const matches = content.matchAll(
        /["']?data-cy["']?\s*[=:]\s*{?["'`]([A-Za-z0-9-_\s]+)["'`]?}?/g
      );
      this.fileItems[file] = [];
      for (let match of matches) {
        this.fileItems[file].push(match[1]);
      }
    }
  }

  async getFiles(dir) {
    const dirents = await readdir(dir, { withFileTypes: true });
    const files = await Promise.all(
      dirents.map((dirent) => {
        const res = resolve(dir, dirent.name);
        return dirent.isDirectory() ? this.getFiles(res) : res;
      })
    );
    return Array.prototype.concat(...files);
  }
}

module.exports = { DataCyPlugin };
