import { execSync } from 'child_process';
import { writeFileSync } from 'fs';
import { join } from 'path';

// Get current branch name
const branchName = execSync('git rev-parse --abbrev-ref HEAD')
  .toString()
  .trim();

// Create JSON content
const content = {
  branchName,
};

writeFileSync(
  join(import.meta.dirname, '..', 'src', 'branch.json'),
  JSON.stringify(content, null, 2)
);
