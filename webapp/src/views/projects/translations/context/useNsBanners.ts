import { useMemo } from 'react';
import { useTranslationsSelector } from './TranslationsContext';

export const useNsBanners = () => {
  const translations = useTranslationsSelector((c) => c.translations);
  return useMemo(() => {
    const nsBanners = [] as { name: string; row: number }[];
    let lastNamespace: undefined | string = undefined;
    translations?.forEach((translation, i) => {
      const keyNamespace = translation.keyNamespace;
      if (
        lastNamespace !== keyNamespace &&
        (keyNamespace || lastNamespace !== undefined)
      ) {
        nsBanners.push({
          name: keyNamespace || '',
          row: i,
        });
      }
      lastNamespace = keyNamespace;
    });
    return nsBanners;
  }, [translations]);
};
