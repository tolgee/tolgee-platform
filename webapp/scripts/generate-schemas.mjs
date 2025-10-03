import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { exec } from 'node:child_process';
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
  throw new Error('Invalid definition');
}

const command = `openapi-typescript ${apiUrl}/v3/api-docs/${definition.schema} --output ${definition.output}`;

exec(command, (error, stdout, stderr) => {
  if (error) {
    console.log(`error: ${error.message}`);
    return;
  }
  if (stderr) {
    console.log(`stderr: ${stderr}`);
    return;
  }
  console.log(`stdout: ${stdout}`);
});
