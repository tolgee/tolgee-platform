import { describe, expect, it } from 'vitest';

import { pickBatchJobStatus } from '../utils';

describe('pickBatchJobStatus', () => {
  it('keeps a finished status when a late RUNNING arrives', () => {
    expect(pickBatchJobStatus('RUNNING', 'SUCCESS')).toBe('SUCCESS');
  });

  it('takes a finished status over a stale RUNNING', () => {
    expect(pickBatchJobStatus('RUNNING', 'FAILED')).toBe('FAILED');
  });

  it('prefers the first status while neither is finished', () => {
    expect(pickBatchJobStatus('RUNNING', 'PENDING')).toBe('RUNNING');
  });

  it('falls back to the other status when the first is missing', () => {
    expect(pickBatchJobStatus(undefined, 'PENDING')).toBe('PENDING');
  });
});
