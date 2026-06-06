import { generateCurrentValue, resolvePluralParameter } from '../utils';
import {
  DeletableKeyWithTranslationsModelType,
  EditorProps,
} from '../../types';

describe('resolvePluralParameter', () => {
  it('prefers the stored arg name', () => {
    expect(resolvePluralParameter('dogsCount', 'count')).toBe('dogsCount');
  });

  it('falls back to the parsed arg name when stored one is missing', () => {
    expect(resolvePluralParameter(undefined, 'count')).toBe('count');
  });

  it('treats an empty stored arg name as missing', () => {
    expect(resolvePluralParameter('', 'count')).toBe('count');
  });

  it('falls back to "value" when nothing is available', () => {
    expect(resolvePluralParameter(undefined, undefined)).toBe('value');
    expect(resolvePluralParameter('', '')).toBe('value');
  });
});

describe('generateCurrentValue', () => {
  const position: EditorProps = { keyId: 1, language: 'en' };

  const pluralKey = (
    keyPluralArgName: string | undefined
  ): DeletableKeyWithTranslationsModelType =>
    ({
      keyIsPlural: true,
      keyPluralArgName,
    } as unknown as DeletableKeyWithTranslationsModelType);

  it('keeps the arg name parsed from the ICU string when the key has none', () => {
    const result = generateCurrentValue(
      position,
      '{count, plural, one {# item} other {# items}}',
      pluralKey(undefined),
      false
    );
    expect(result.value.parameter).toBe('count');
  });

  it('prefers the stored arg name over the parsed one', () => {
    const result = generateCurrentValue(
      position,
      '{count, plural, one {# item} other {# items}}',
      pluralKey('dogsCount'),
      false
    );
    expect(result.value.parameter).toBe('dogsCount');
  });

  it('falls back to "value" when the text is not a parseable plural', () => {
    const result = generateCurrentValue(
      position,
      'just some text',
      pluralKey(undefined),
      false
    );
    expect(result.value.parameter).toBe('value');
  });
});
