import { useMemo } from 'react';
import { useTranslationsSelector } from './TranslationsContext';

export const useNsBanners = () => {
  const translations = useTranslationsSelector((c) => c.translations);
  return useMemo(() => {
    const nsBanners = [] as { name: string | undefined; row: number }[];
    let lastNamespace: null | undefined | string = null;
    translations?.forEach((translation, i) => {
      const keyNamespace = translation.keyNamespace;
      if (
        lastNamespace !== keyNamespace &&
        (keyNamespace || lastNamespace !== null)
      ) {
        nsBanners.push({
          name: keyNamespace || '<default>',
          row: i,
        });
      }
      lastNamespace = keyNamespace;
    });
    return nsBanners;
  }, [translations]);
};
