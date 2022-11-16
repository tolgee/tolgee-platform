import { useMemo } from 'react';
import { useTranslationsSelector } from './TranslationsContext';

export type NsBannerRecord = {
  name: string;
  id: number | undefined;
  row: number;
};

export const useNsBanners = () => {
  const translations = useTranslationsSelector((c) => c.translations);
  return useMemo(() => {
    const nsBanners = [] as NsBannerRecord[];
    let lastNamespace: undefined | string = undefined;
    translations?.forEach((translation, i) => {
      const keyNamespace = translation.keyNamespace;
      const id = translation.keyNamespaceId;
      if (
        lastNamespace !== keyNamespace &&
        (keyNamespace || lastNamespace !== undefined)
      ) {
        nsBanners.push({
          name: keyNamespace || '',
          id,
          row: i,
        });
      }
      lastNamespace = keyNamespace;
    });
    return nsBanners;
  }, [translations]);
};
