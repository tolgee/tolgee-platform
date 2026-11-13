import { describe, expect, it } from 'vitest';

import { BatchJobModel, BatchJobStatus } from '../../types';
import { applyProgress, mergeServerJobs } from '../utils';

const progress = (status: BatchJobStatus) => ({
  jobId: 1,
  processed: 1,
  total: 1,
  status,
  errorMessage: undefined,
});

const serverJob = (status: BatchJobStatus) =>
  ({ id: 1, status, progress: 1, totalItems: 1 } as BatchJobModel);

describe('applyProgress', () => {
  it('adds a job once when two events arrive together', () => {
    const jobs = applyProgress(
      applyProgress([], progress('RUNNING')),
      progress('SUCCESS')
    );
    expect(jobs).toHaveLength(1);
    expect(jobs[0].status).toBe('SUCCESS');
  });

  it('keeps a finished job when a late RUNNING arrives', () => {
    const jobs = applyProgress(
      applyProgress([], progress('SUCCESS')),
      progress('RUNNING')
    );
    expect(jobs[0].status).toBe('SUCCESS');
  });
});

describe('mergeServerJobs', () => {
  it('takes a finished status from the server', () => {
    const jobs = applyProgress([], progress('RUNNING'));
    expect(mergeServerJobs(jobs, [serverJob('SUCCESS')])[0].status).toBe(
      'SUCCESS'
    );
  });

  it('keeps a live finished status over a stale server RUNNING', () => {
    const jobs = applyProgress([], progress('SUCCESS'));
    expect(mergeServerJobs(jobs, [serverJob('RUNNING')])[0].status).toBe(
      'SUCCESS'
    );
  });
});
