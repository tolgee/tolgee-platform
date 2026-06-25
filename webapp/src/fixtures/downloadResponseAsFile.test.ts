import {
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
