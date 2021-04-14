const {resolve} = require('path');
const {readdir, readFile, writeFile} = require('fs').promises;
const path = require("path");

/**
 * Plugin generating types for Cypress data-cy items
 */
class DataCyPlugin {

    fileItems = {}

    apply(compiler) {
        const srcPath = path.resolve("./src");
        // Specify the event hook to attach to
        compiler.hooks.afterPlugins.tap("DataCyPlugin", async () => {
            console.log("Generating Data Cy type...")
            const files = await this.getFiles(srcPath)
            await this.generate(files);
        })

        compiler.hooks.watchRun.tapPromise('DataCyPluginWatch', async (comp) => {
            const changedTimes = comp.watchFileSystem.watcher.mtimes;
            const changedFiles = Object.keys(changedTimes)
            console.log("watch")
            if (changedFiles.length) {
                await this.generate(changedFiles)
            }
        });
    }

    async generate(files) {
        for (let file of files) {
            if (/.*\.tsx$/.test(file)) {
                const content = (await readFile(file)).toString()
                const matches = content.matchAll(/data-cy={?["'`]([A-Za-z0-9-_\s]+)["'`]?}?/g)
                for (let match of matches) {
                    this.fileItems[file] = this.fileItems[file] ? [...this.fileItems[file], match[1]] : [match[1]]
                }
            }
        }

        const items = Object.values(this.fileItems).reduce((acc, curr) => [...acc, ...curr], [])
        let fileContent = "declare namespace DataCy {\n"
        fileContent += "    export type Value = \n        " + items.map(i => `"${i}"`).join(" |\n        ") + "\n}";
        await writeFile(path.resolve(`../e2e/cypress/support/dataCyType.d.ts`), fileContent)
        console.log("Data Cy type generated")
    }

    async getFiles(dir) {
        const dirents = await readdir(dir, {withFileTypes: true});
        const files = await Promise.all(dirents.map((dirent) => {
            const res = resolve(dir, dirent.name);
            return dirent.isDirectory() ? this.getFiles(res) : res;
        }));
        return Array.prototype.concat(...files);
    }
}

module.exports = {DataCyPlugin}
