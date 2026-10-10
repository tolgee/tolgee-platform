import { detectCountry } from './detectCountry';

function withLanguages(languages: string[] | undefined) {
  vi.spyOn(navigator, 'languages', 'get').mockReturnValue(
    languages as string[]
  );
  vi.spyOn(navigator, 'language', 'get').mockReturnValue(languages?.[0] ?? '');
}

describe('detectCountry', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('reads the region out of the first tag that carries one', () => {
    withLanguages(['cs-CZ', 'en-US']);
    expect(detectCountry()).toEqual('CZ');
  });

  it('skips a language-only tag and uses the next one', () => {
    withLanguages(['cs', 'de-AT']);
    expect(detectCountry()).toEqual('AT');
  });

  it('skips a UN M49 region, which is not a country', () => {
    withLanguages(['es-419', 'es-MX']);
    expect(detectCountry()).toEqual('MX');
  });

  it('skips a region libphonenumber does not know', () => {
    withLanguages(['en-ZZ', 'en-GB']);
    expect(detectCountry()).toEqual('GB');
  });

  it('uppercases a lowercase region', () => {
    withLanguages(['fr-fr']);
    expect(detectCountry()).toEqual('FR');
  });

  it('falls back to US when no tag carries a usable region', () => {
    withLanguages(['cs', 'en']);
    expect(detectCountry()).toEqual('US');
  });

  it('falls back to US when the browser reports no languages at all', () => {
    withLanguages([]);
    expect(detectCountry()).toEqual('US');
  });

  it('uses navigator.language when navigator.languages is undefined', () => {
    withLanguages(undefined);
    vi.spyOn(navigator, 'language', 'get').mockReturnValue('pt-BR');
    expect(detectCountry()).toEqual('BR');
  });
});
