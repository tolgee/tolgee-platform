import { vi } from 'vitest';

import { captureDownloadAnchor } from 'tg.fixtures/captureDownloadAnchor.testUtils';

import { downloadExported } from './useGlossaryExport';

const blobResponse = (): Response =>
  ({ blob: async () => new Blob(['x']) } as unknown as Response);

describe('useGlossaryExport downloadExported filename', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-14T10:00:00Z'));
  });
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('includes the branch segment when a branch is given', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(blobResponse(), 'Name', 'main');
    expect(anchor.download).toBe('glossary_Name(main)_2026-07-14.csv');
  });

  it('omits the branch segment when no branch is given', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(blobResponse(), 'Name');
    expect(anchor.download).toBe('glossary_Name_2026-07-14.csv');
  });
});
