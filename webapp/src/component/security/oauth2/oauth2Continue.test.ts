import { isSafeContinue } from './oauth2Continue';

const API_URL = 'https://app.example.com';

describe('isSafeContinue', () => {
  it('accepts a same-origin /oauth2/authorize target', () => {
    expect(
      isSafeContinue('https://app.example.com/oauth2/authorize?x=1', API_URL)
    ).toBe(true);
  });

  it('rejects a foreign origin', () => {
    expect(
      isSafeContinue('https://evil.example.com/oauth2/authorize', API_URL)
    ).toBe(false);
  });

  it('rejects the right origin but a different path', () => {
    expect(isSafeContinue('https://app.example.com/evil', API_URL)).toBe(false);
  });

  it('rejects undefined and malformed input', () => {
    expect(isSafeContinue(undefined, API_URL)).toBe(false);
    expect(isSafeContinue('not-a-url', API_URL)).toBe(false);
  });

  it('falls back to window.location.origin when apiUrl is empty', () => {
    const origin = window.location.origin;
    expect(isSafeContinue(`${origin}/oauth2/authorize`, '')).toBe(true);
    expect(
      isSafeContinue('https://evil.example.com/oauth2/authorize', '')
    ).toBe(false);
  });
});
