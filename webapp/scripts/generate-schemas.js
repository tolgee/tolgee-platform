import { fileURLToPath } from 'node:url';
import { basename, dirname, resolve } from 'node:path';
import { execSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import process from 'node:process';
import { config } from 'dotenv-flow';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

config({
  default_node_env: 'development',
  path: resolve(__dirname, '../'),
});

const apiUrl = process.env.VITE_APP_API_URL || 'http://localhost:8080';

const definitions = {
  public: {
    schema: 'All%20Internal%20-%20for%20Tolgee%20Web%20application',
    output: './src/service/apiSchema.generated.ts',
  },
  billing: {
    schema: 'V2%20Billing',
    output: './src/service/billingApiSchema.generated.ts',
  },
};

const definition = definitions[process.argv[2]];
if (!definition) {
  throw new Error(
    `Invalid definition '${process.argv[2] ?? ''}'. Valid options: ${Object.keys(definitions).join(', ')}`
  );
}

const specUrl = `${apiUrl}/v3/api-docs/${definition.schema}`;

function generateSchema() {
  const command = `openapi-typescript ${specUrl} --output ${definition.output}`;
  console.log(`Running: ${command}`);
  execSync(command, { stdio: 'inherit' });
}

async function generateSchemaTypes() {
  const response = await fetch(specUrl);
  if (!response.ok) {
    throw new Error(
      `Failed to fetch OpenAPI spec from ${specUrl}: ${response.status} ${response.statusText}`
    );
  }
  const spec = await response.json();
  const schemaNames = Object.keys(spec.components.schemas).sort();

  const schemaBasename = basename(definition.output, '.ts');
  const typesOutput = definition.output.replace(
    '.generated.ts',
    'Types.generated.ts'
  );

  const lines = [
    '/**',
    ' * This file was auto-generated from the OpenAPI schema types.',
    ' * Do not make direct changes to the file.',
    ' */',
    `import type { components } from './${schemaBasename}';`,
    '',
    ...schemaNames.map(
      (name) => `export type ${name} = components['schemas']['${name}'];`
    ),
    '',
  ];

  writeFileSync(typesOutput, lines.join('\n'));
  console.log(`Generated ${schemaNames.length} type aliases in ${typesOutput}`);
}

generateSchema();
await generateSchemaTypes();
console.log('Schema generation completed.');
