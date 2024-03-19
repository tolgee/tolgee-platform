import {
  getTolgeePlurals,
  getVariantExample,
  selectPluralRule,
} from '@tginternal/editor';
import { useMemo } from 'react';
import { useProject } from 'tg.hooks/useProject';

export const useExtractedPlural = (
  variant: string | undefined,
  text: string | undefined
) => {
  const project = useProject();
  return useMemo(() => {
    if (variant && text) {
      const plurals = getTolgeePlurals(text, !project.icuPlaceholders);
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
      return selectPluralRule(baseLanguage, example);
    }
  }, [baseLanguage, example]);
};
