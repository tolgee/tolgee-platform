import { components } from 'tg.service/apiSchema.generated';
import { CellPosition, ViewMode } from '../types';

type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];

type ArrayElement<A> = A extends readonly (infer T)[] ? T : never;

export const ARROWS = [
  'ArrowUp',
  'ArrowDown',
  'ArrowLeft',
  'ArrowRight',
] as const;

export const getCurrentlyFocused = (
  elements: Map<string, HTMLElement> | undefined | null
): CellPosition | undefined => {
  if (elements) {
    for (const [key, el] of elements.entries()) {
      if (el.contains(document.activeElement)) {
        return JSON.parse(key);
      }
    }
  }
  return undefined;
};

export const translationsNavigator = (
  fixedTranslations: KeyWithTranslationsModelType[] | undefined,
  languages: string[] | undefined,
  elements: Map<string, HTMLElement> | undefined | null,
  view: ViewMode,
  visibleRange: number[] | undefined
) => {
  const getNextLocation = (
    arrow: ArrayElement<typeof ARROWS>
  ): { keyId: number; language: string | undefined } | undefined => {
    if (!languages || !fixedTranslations) {
      return undefined;
    }
    const focused = getCurrentlyFocused(elements);
    let nextKeyId: number | undefined;
    let nextLanguage: string | undefined;

    if (!focused) {
      nextKeyId = getInitialKeyId();
      nextLanguage = languages[0];
    } else {
      nextKeyId = focused.keyId;
      nextLanguage = focused?.language;
      if (view === 'TABLE') {
        switch (arrow) {
          case 'ArrowDown':
            nextKeyId = moveInTranslations(focused.keyId, 1);
            break;
          case 'ArrowUp':
            nextKeyId = moveInTranslations(focused.keyId, -1);
            break;
          case 'ArrowRight':
            nextLanguage = focused.language
              ? isLanguageLast(focused.language)
                ? focused.language
                : moveInLaguages(focused.language, 1)
              : languages[0];
            break;
          case 'ArrowLeft':
            nextLanguage =
              !focused.language || isLanguageFirst(focused.language)
                ? undefined
                : moveInLaguages(focused.language, -1);
            break;
        }
      } else {
        switch (arrow) {
          case 'ArrowDown':
            if (focused.language) {
              if (
                isLanguageLast(focused.language) &&
                !isTranslationLast(focused.keyId)
              ) {
                nextKeyId = moveInTranslations(focused.keyId, 1);
                nextLanguage = languages[0];
              } else {
                nextLanguage = moveInLaguages(focused.language, 1);
              }
            } else {
              nextKeyId = moveInTranslations(focused.keyId, 1);
            }
            break;
          case 'ArrowUp':
            if (focused.language) {
              if (
                isLanguageFirst(focused.language) &&
                !isTranslationFirst(focused.keyId)
              ) {
                nextKeyId = moveInTranslations(focused.keyId, -1);
                nextLanguage = languages[languages.length - 1];
              } else {
                nextLanguage = moveInLaguages(focused.language, -1);
              }
            } else {
              nextKeyId = moveInTranslations(focused.keyId, -1);
            }
            break;
          case 'ArrowRight':
            if (!focused.language) {
              nextLanguage = languages[0];
            }
            break;
          case 'ArrowLeft':
            nextLanguage = undefined;
            break;
        }
      }
    }
    return nextKeyId !== undefined
      ? { keyId: nextKeyId, language: nextLanguage }
      : undefined;
  };

  const getInitialKeyId = (): number | undefined => {
    const [first, last] = visibleRange || [0, 0];
    if (first === 0) {
      return fixedTranslations?.[0]?.keyId;
    } else {
      const index = Math.round((first + last) / 2);
      return fixedTranslations?.[index]?.keyId;
    }
  };

  const isLanguageFirst = (lang: string) => languages?.indexOf(lang) === 0;

  const isLanguageLast = (lang: string) =>
    languages && languages.indexOf(lang) === languages.length - 1;

  const isTranslationFirst = (keyId: number) =>
    fixedTranslations?.findIndex((t) => t.keyId === keyId) === 0;

  const isTranslationLast = (keyId: number) =>
    fixedTranslations &&
    fixedTranslations.findIndex((t) => t.keyId === keyId) ===
      fixedTranslations.length - 1;

  const moveInLaguages = (lang: string, index: 1 | -1) => {
    if (languages) {
      const currentIndex = languages?.indexOf(lang);
      return (
        (currentIndex !== -1 ? languages[currentIndex + index] : undefined) ||
        lang
      );
    }
    return undefined;
  };
  const moveInTranslations = (keyId: number, index: 1 | -1) => {
    if (fixedTranslations) {
      const currentIndex = fixedTranslations?.findIndex(
        (t) => t.keyId === keyId
      );
      return (
        (currentIndex !== -1
          ? fixedTranslations[currentIndex + index]?.keyId
          : undefined) || keyId
      );
    }
    return undefined;
  };

  return {
    getNextLocation,
  };
};

export const serializeElPosition = (position: CellPosition) => {
  return JSON.stringify({
    keyId: position.keyId,
    language: position.language,
  });
};
