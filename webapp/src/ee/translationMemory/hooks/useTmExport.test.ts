import { vi } from 'vitest';

import { captureDownloadAnchor } from 'tg.fixtures/captureDownloadAnchor.testUtils';

import { downloadExported } from './useTmExport';

const blobResponse = (): Response =>
  ({ blob: async () => new Blob(['x']) } as unknown as Response);

describe('useTmExport downloadExported filename', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-14T10:00:00Z'));
  });
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('builds a date-suffixed tmx name from a plain name', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(blobResponse(), 'Name');
    expect(anchor.download).toBe('tm_Name_2026-07-14.tmx');
  });

  it('replaces reserved chars and collapses whitespace', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(blobResponse(), 'a b*c');
    expect(anchor.download).toBe('tm_a_b_c_2026-07-14.tmx');
  });

  it('falls back to "tm" when the stem sanitizes to empty', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(blobResponse(), '   ');
    expect(anchor.download).toBe('tm_tm_2026-07-14.tmx');
  });
});
