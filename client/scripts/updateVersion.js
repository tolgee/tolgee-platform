import fs from "node:fs";

const packageJson = fs.readFileSync("./package.json");

const version = String(process.argv[2]).replace(/^v/, "");

const packageData = JSON.parse(packageJson);
packageData.version = version;
const replaced = JSON.stringify(packageData, null, 2) + "\n";

fs.writeFileSync("./package.json", replaced);

const content = `
/**
 * This file is generated automatically by \`updateVersion.js\` script
 */
export const VERSION = "${version}";
`.trimStart();

fs.writeFileSync("./src/version.ts", content);
