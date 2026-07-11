import * as fs from 'fs';
import * as path from 'path';

/**
 * Registers an `after:run` handler that records which e2e tests were flaky and
 * which failed outright, so the reportIntermittentTests workflow can tell them
 * apart.
 *
 * A test is flaky when it failed at least one attempt but ultimately passed;
 * it is a hard failure when every attempt failed. Two files are written to the
 * e2e working directory:
 *
 *   - flaky-tests-e2e-<index>.txt   flaky tests -> reported to the project board
 *   - failed-tests-e2e-<index>.txt  hard failures -> reported to Slack only
 *
 * Test ids use the `<spec>/<title>` format expected by report-flaky-tests.py.
 */
export function registerFlakyReport(on: Cypress.PluginEvents): void {
  // No-op outside the flaky-detection workflow so local `cypress run` and
  // other CI workflows don't write stray report files.
  if (process.env.DETECT_FLAKY_TESTS !== 'true') {
    return;
  }

  on('after:run', (results) => {
    // A run that failed to start (CypressFailedRunResult) has no `runs`.
    if (!('runs' in results)) {
      return;
    }

    const flaky: string[] = [];
    const failed: string[] = [];

    for (const run of results.runs) {
      for (const test of run.tests) {
        const id = `${run.spec.relative}/${test.title.join(' > ')}`;
        const failedAttempts = test.attempts.filter(
          (attempt) => attempt.state === 'failed'
        ).length;

        if (test.state === 'passed' && failedAttempts > 0) {
          flaky.push(id);
        } else if (test.state === 'failed') {
          failed.push(id);
        }
      }
    }

    const index = process.env.E2E_JOB_INDEX ?? '0';
    writeReport(`flaky-tests-e2e-${index}.txt`, flaky);
    writeReport(`failed-tests-e2e-${index}.txt`, failed);
  });
}

function writeReport(fileName: string, ids: string[]): void {
  const unique = [...new Set(ids)].sort();
  const content = unique.length > 0 ? `${unique.join('\n')}\n` : '';
  fs.writeFileSync(path.resolve(process.cwd(), fileName), content);
}
