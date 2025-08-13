import fs from "node:fs";

const packageJson = fs.readFileSync("./package.json");

const version = String(process.argv[2]).replace(/^v/, "");

const replaced = packageJson
  .toString()
  .replaceAll(/"version": ".*"/g, `"version": "${version}"`);

fs.writeFileSync("./package.json", replaced);

const content = `
/**
 * This file is generated automatically by \`updateVersion.js\` script
 */
export const VERSION = "${version}";
`.trimStart();

fs.writeFileSync("./src/version.ts", content);
