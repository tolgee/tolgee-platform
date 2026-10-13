import {
  ExportFormValues,
  getDefaultExportValues,
  getExportRequestBody,
  getTagFilterParams,
  hasCustomAdvancedSettings,
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

  describe('hasCustomAdvancedSettings', () => {
    const defaults = getDefaultExportValues(LANGUAGES, NAMESPACES);
    const isCustomWith = (changes: Partial<ExportFormValues>) =>
      hasCustomAdvancedSettings({ ...defaults, ...changes }, NAMESPACES);

    it('is false for the defaults', () => {
      expect(hasCustomAdvancedSettings(defaults, NAMESPACES)).toBe(false);
    });

    it('ignores languages and format, which are not advanced', () => {
      expect(isCustomWith({ languages: ['cs'], format: 'generic_xliff' })).toBe(
        false
      );
    });

    it('detects changed states', () => {
      expect(isCustomWith({ states: ['REVIEWED'] })).toBe(true);
      expect(isCustomWith({ states: [] })).toBe(true);
    });

    it('treats an empty namespace selection as all namespaces', () => {
      expect(isCustomWith({ namespaces: [] })).toBe(false);
      expect(isCustomWith({ namespaces: ['ns-1'] })).toBe(true);
    });

    it('detects tag filters', () => {
      expect(isCustomWith({ tagsIn: ['release'] })).toBe(true);
      expect(isCustomWith({ tagsNotIn: ['wip'] })).toBe(true);
    });

    it('detects a message format only for formats offering a choice', () => {
      expect(
        isCustomWith({ format: 'generic_xliff', messageFormat: 'PHP_SPRINTF' })
      ).toBe(true);
      expect(isCustomWith({ messageFormat: 'PHP_SPRINTF' })).toBe(false);
    });

    it('detects support arrays only when enabled', () => {
      expect(
        isCustomWith({
          format: 'generic_structured_json',
          supportArrays: false,
        })
      ).toBe(false);
      expect(
        isCustomWith({ format: 'generic_structured_json', supportArrays: true })
      ).toBe(true);
      expect(isCustomWith({ supportArrays: true })).toBe(false);
    });

    it('detects escaping HTML only for formats offering it', () => {
      expect(isCustomWith({ format: 'generic_xliff', escapeHtml: true })).toBe(
        true
      );
      expect(isCustomWith({ escapeHtml: true })).toBe(false);
    });
  });

  it('drops tags from the exclude filter that are also included', () => {
    const restored = restoreExportValues(
      { tagsIn: ['release', 'web'], tagsNotIn: ['release', 'wip'] },
      LANGUAGES,
      NAMESPACES
    );
    expect(restored.tagsIn).toEqual(['release', 'web']);
    expect(restored.tagsNotIn).toEqual(['wip']);
  });

  it('sends no message format for a format without a choice', () => {
    const defaults = getDefaultExportValues(LANGUAGES, NAMESPACES);
    const leftover = { ...defaults, messageFormat: 'PHP_SPRINTF' as const };
    expect(getExportRequestBody(leftover, undefined).messageFormat).toBe(
      undefined
    );
    expect(
      getExportRequestBody({ ...leftover, format: 'generic_xliff' }, undefined)
        .messageFormat
    ).toBe('PHP_SPRINTF');
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
