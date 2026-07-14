import { vi } from 'vitest';

import { captureDownloadAnchor } from 'tg.fixtures/captureDownloadAnchor.testUtils';

import { FormatItem } from './components/formatGroups';
import { downloadExported } from './downloadExported';

const format = { extension: 'json' } as unknown as FormatItem;

const exportResponse = (
  type: string,
  contentDisposition: string | null = null
): Response =>
  ({
    blob: async () => new Blob(['x'], { type }),
    headers: {
      get: (name: string) =>
        name === 'Content-Disposition' ? contentDisposition : null,
    },
  } as unknown as Response);

describe('downloadExported filename', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-14T10:00:00Z'));
  });
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('names a zip archive without an extension guess', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(
      exportResponse('application/zip'),
      ['en', 'de'],
      format,
      'Proj',
      'main'
    );
    expect(anchor.download).toBe('Proj(main)_2026-07-14.zip');
  });

  it('omits the branch segment when no branch is given', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(
      exportResponse('application/zip'),
      ['en'],
      format,
      'Proj'
    );
    expect(anchor.download).toBe('Proj_2026-07-14.zip');
  });

  it('omits the language suffix for a single-language zip export', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(
      exportResponse('application/zip'),
      ['en'],
      format,
      'Proj',
      'main'
    );
    expect(anchor.download).toBe('Proj(main)_2026-07-14.zip');
  });

  it('adds a single-language suffix and the format extension', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(exportResponse(''), ['en'], format, 'Proj');
    expect(anchor.download).toBe('Proj_en_2026-07-14.json');
  });

  it('orders the branch segment before the single-language suffix', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(exportResponse(''), ['en'], format, 'Proj', 'main');
    expect(anchor.download).toBe('Proj(main)_en_2026-07-14.json');
  });

  it('omits the language suffix for multiple languages', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(exportResponse(''), ['en', 'de'], format, 'Proj');
    expect(anchor.download).toBe('Proj_2026-07-14.json');
  });

  it('falls back to the format extension when Content-Disposition has no extension', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(
      exportResponse('', 'attachment; filename="noext"'),
      ['en'],
      format,
      'Proj'
    );
    expect(anchor.download).toBe('Proj_en_2026-07-14.json');
  });

  it('prefers the Content-Disposition extension over the format extension', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadExported(
      exportResponse('', 'attachment; filename="whatever.xliff"'),
      ['en'],
      format,
      'Proj'
    );
    expect(anchor.download).toBe('Proj_en_2026-07-14.xliff');
  });
});
