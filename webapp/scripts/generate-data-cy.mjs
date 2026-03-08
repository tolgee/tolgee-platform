import { resolve } from 'node:path';
import { generateAll } from '../dataCy.core.mjs';

const srcPath = resolve(import.meta.dirname, '../src');
const outputPath = resolve(
  import.meta.dirname,
  '../../e2e/cypress/support/dataCyType.d.ts'
);

await generateAll(srcPath, outputPath);
