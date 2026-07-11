import { execSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

// Get current branch name
const branchName = execSync('git rev-parse --abbrev-ref HEAD')
  .toString()
  .trim();

// Create JSON content
const content = {
  branchName,
};

writeFileSync(
  join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'branch.json'),
  JSON.stringify(content, null, 2)
);
