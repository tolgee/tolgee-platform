import {
  getDefaultExportValues,
  getTagFilterParams,
  isSameExportSetup,
  readExportSettingsFromUrl,
  restoreExportValues,
  toStoredExportSettings,
} from 'tg.views/projects/export/exportSettings';

const LANGUAGES = ['en', 'cs', 'de'];
const NAMESPACES = ['', 'ns-1'];

describe('export settings', () => {
  it('defaults to all states, languages and namespaces', () => {
    const defaults = getDefaultExportValues(LANGUAGES, NAMESPACES);
    expect(defaults.states).toEqual(['UNTRANSLATED', 'TRANSLATED', 'REVIEWED']);
    expect(defaults.languages).toEqual(LANGUAGES);
    expect(defaults.namespaces).toEqual(NAMESPACES);
    expect(defaults.tagsIn).toEqual([]);
  });

  it('stores "everything selected" as null so items added later are included', () => {
    const values = getDefaultExportValues(LANGUAGES, NAMESPACES);
    const stored = toStoredExportSettings(values, LANGUAGES, NAMESPACES);
    expect(stored.languages).toBeNull();
    expect(stored.namespaces).toBeNull();

    const restored = restoreExportValues(
      stored,
      [...LANGUAGES, 'fr'],
      [...NAMESPACES, 'ns-2']
    );
    expect(restored.languages).toEqual([...LANGUAGES, 'fr']);
    expect(restored.namespaces).toEqual([...NAMESPACES, 'ns-2']);
  });

  it('stores a partial selection as the selected items', () => {
    const values = {
      ...getDefaultExportValues(LANGUAGES, NAMESPACES),
      languages: ['cs'],
      namespaces: ['ns-1'],
    };
    const stored = toStoredExportSettings(values, LANGUAGES, NAMESPACES);
    expect(stored.languages).toEqual(['cs']);
    expect(stored.namespaces).toEqual(['ns-1']);
  });

  it('drops remembered items that no longer exist', () => {
    const restored = restoreExportValues(
      {
        languages: ['cs', 'fr'],
        namespaces: ['gone'],
        states: ['REVIEWED', 'DISABLED' as any],
        format: 'removed_format',
      },
      LANGUAGES,
      NAMESPACES
    );
    expect(restored.languages).toEqual(['cs']);
    expect(restored.namespaces).toEqual(NAMESPACES);
    expect(restored.states).toEqual(['REVIEWED']);
    expect(restored.format).toEqual(
      getDefaultExportValues(LANGUAGES, NAMESPACES).format
    );
  });

  it('reads only the settings present in the url', () => {
    expect(
      readExportSettingsFromUrl('?languages=cs&tagsNotIn=wip&tagsNotIn=old')
    ).toEqual({ languages: ['cs'], tagsNotIn: ['wip', 'old'] });
    expect(readExportSettingsFromUrl('')).toEqual({});
  });

  it('lets url settings override only the matching remembered ones', () => {
    const restored = restoreExportValues(
      {
        ...{ format: 'generic_xliff', tagsIn: ['release'] },
        ...readExportSettingsFromUrl('?languages=cs'),
      },
      LANGUAGES,
      NAMESPACES
    );
    expect(restored.format).toEqual('generic_xliff');
    expect(restored.tagsIn).toEqual(['release']);
    expect(restored.languages).toEqual(['cs']);
  });

  it('compares setups regardless of item order', () => {
    const a = getDefaultExportValues(LANGUAGES, NAMESPACES);
    const b = { ...a, languages: [...LANGUAGES].reverse() };
    expect(isSameExportSetup(a, b)).toBe(true);
    expect(isSameExportSetup(a, { ...a, tagsIn: ['x'] })).toBe(false);
  });

  it('ignores a leftover message format of a format without message formats', () => {
    const defaults = getDefaultExportValues(LANGUAGES, NAMESPACES);
    const withLeftover = { ...defaults, messageFormat: 'PHP_SPRINTF' as const };
    expect(isSameExportSetup(defaults, withLeftover)).toBe(true);
    expect(
      toStoredExportSettings(withLeftover, LANGUAGES, NAMESPACES).messageFormat
    ).toBeUndefined();
  });

  it('omits empty tag filters from the request', () => {
    expect(getTagFilterParams({ tagsIn: [], tagsNotIn: [] })).toEqual({
      filterTagIn: undefined,
      filterTagNotIn: undefined,
    });
    expect(getTagFilterParams({ tagsIn: ['a'], tagsNotIn: [] })).toEqual({
      filterTagIn: ['a'],
      filterTagNotIn: undefined,
    });
  });
});
