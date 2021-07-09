import { InfiniteData } from 'react-query';
import { components } from 'tg.service/apiSchema.generated';

type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];
type TranslationsResponse =
  components['schemas']['KeysWithTranslationsPageModel'];

export const updateTranslationKey = (
  keys: KeyWithTranslationsModelType[],
  keyId: number,
  value: Partial<KeyWithTranslationsModelType>
) => {
  return keys.map((k) => {
    if (k.keyId === keyId) {
      return { ...k, ...value };
    } else {
      return k;
    }
  });
};

export const updateTranslation = (
  keys: KeyWithTranslationsModelType[],
  keyId: number,
  language: string,
  value: string
) => {
  return keys.map((k) => {
    if (k.keyId === keyId) {
      return {
        ...k,
        translations: {
          ...k.translations,
          [language]: {
            ...k.translations[language],
            text: value,
          },
        },
      };
    } else {
      return k;
    }
  });
};

export const flattenKeys = (
  data: InfiniteData<TranslationsResponse>
): KeyWithTranslationsModelType[] =>
  data?.pages.filter(Boolean).flatMap((p) => p._embedded?.keys || []) || [];
