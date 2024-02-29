import { getTolgeePlurals, getVariantExample } from '@tginternal/editor';
import { useMemo } from 'react';

export const useExtractedPlural = (
  variant: string | undefined,
  text: string | undefined
) => {
  return useMemo(() => {
    if (variant && text) {
      const plurals = getTolgeePlurals(text);
      if (plurals.parameter) {
        return plurals.variants[variant] || '';
      }
    }
    return text;
  }, [variant, text]);
};

export const useVariantExample = (
  variant: string | undefined,
  language: string
) => {
  return useMemo(() => {
    if (!variant) {
      return undefined;
    }
    return getVariantExample(language, variant);
  }, [variant, language]);
};

export const useBaseVariant = (
  variant: string | undefined,
  language: string,
  baseLanguage: string
) => {
  const example = useVariantExample(variant, language);

  return useMemo(() => {
    if (example) {
      return new Intl.PluralRules(baseLanguage).select(example);
    }
  }, [baseLanguage, example]);
};
