import { vi } from 'vitest';

import { captureDownloadAnchor } from './captureDownloadAnchor.testUtils';
import {
  downloadBlobAsFile,
  downloadResponseAsFile,
  parseContentDispositionFilename,
  sanitizeFilename,
} from './downloadResponseAsFile';

const responseWith = (contentDisposition: string | null): Response =>
  ({
    headers: {
      get: (name: string) =>
        name === 'Content-Disposition' ? contentDisposition : null,
    },
  } as unknown as Response);

const fileResponse = (
  contentDisposition: string | null,
  body = 'x'
): Response =>
  ({
    blob: async () => new Blob([body]),
    headers: {
      get: (name: string) =>
        name === 'Content-Disposition' ? contentDisposition : null,
    },
  } as unknown as Response);

describe('parseContentDispositionFilename', () => {
  it('returns null when the header is absent', () => {
    expect(parseContentDispositionFilename(responseWith(null))).toBeNull();
  });

  it('extracts a quoted filename', () => {
    expect(
      parseContentDispositionFilename(
        responseWith('attachment; filename="My Project.zip"')
      )
    ).toBe('My Project.zip');
  });

  it('extracts an unquoted filename', () => {
    expect(
      parseContentDispositionFilename(
        responseWith('attachment; filename=MyProject.zip')
      )
    ).toBe('MyProject.zip');
  });

  it('returns null when the header carries no filename', () => {
    expect(
      parseContentDispositionFilename(responseWith('attachment'))
    ).toBeNull();
  });
});

describe('sanitizeFilename', () => {
  it('passes a clean name through unchanged', () => {
    expect(sanitizeFilename('My Project.zip')).toBe('My Project.zip');
  });

  it('replaces path and reserved characters with underscores', () => {
    expect(sanitizeFilename('a/b\\c:d*e?.zip')).toBe('a_b_c_d_e_.zip');
  });

  it('falls back to "download" when nothing usable remains', () => {
    expect(sanitizeFilename('')).toBe('download');
    expect(sanitizeFilename('   ')).toBe('download');
  });
});

describe('downloadBlobAsFile', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('downloads the blob under the given filename and defers the revoke', () => {
    const { anchor, revokeObjectURL } = captureDownloadAnchor();
    downloadBlobAsFile(new Blob(['x']), 'My File.zip');

    expect(anchor.download).toBe('My File.zip');
    expect(anchor.click).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).not.toHaveBeenCalled();

    vi.advanceTimersByTime(7000);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock');
  });
});

describe('downloadResponseAsFile', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('uses the Content-Disposition filename when present', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadResponseAsFile(
      fileResponse('attachment; filename="Server Name.zip"'),
      'fallback.zip'
    );
    expect(anchor.download).toBe('Server Name.zip');
  });

  it('falls back to the given name and sanitizes it when no header', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadResponseAsFile(fileResponse(null), 'a/b:c.zip');
    expect(anchor.download).toBe('a_b_c.zip');
  });

  it('sanitizes reserved characters in the parsed server filename', async () => {
    const { anchor } = captureDownloadAnchor();
    await downloadResponseAsFile(
      fileResponse('attachment; filename="a/b:c.zip"'),
      'fallback.zip'
    );
    expect(anchor.download).toBe('a_b_c.zip');
  });
});
