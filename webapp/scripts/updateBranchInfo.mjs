import { execSync } from 'child_process';
import { writeFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

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
