import { useState } from 'react';

import { useTranslationsService } from './useTranslationsService';

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
};

export const useSelectionService = ({ translations }: Props) => {
  const [selection, setSelection] = useState<number[]>([]);

  const toggle = (keyId: number) => {
    const newSelection = selection.includes(keyId)
      ? selection.filter((s) => s !== keyId)
      : [...selection, keyId];
    setSelection(newSelection);
  };

  const select = (data: number[]) => {
    setSelection(data);
  };

  const groupToggle = (keyId: number) => {
    const lastSelected = selection[selection.length - 1];
    const lastIndex =
      translations.fixedTranslations?.findIndex(
        (t) => t.keyId === lastSelected
      ) ?? -1;
    const currentIndex =
      translations.fixedTranslations?.findIndex((t) => t.keyId === keyId) ?? -1;

    if (lastIndex < 0 || currentIndex < 0) {
      toggle(keyId);
      return;
    }

    let from = lastIndex;
    let to = currentIndex;

    if (lastIndex > currentIndex) {
      from = currentIndex;
      to = lastIndex;
    }
    const keys =
      translations.fixedTranslations
        ?.filter((_, i) => i >= from && i <= to)
        .map((k) => k.keyId)
        .filter((id) => !selection.includes(id)) ?? [];
    setSelection((selected) => [...selected, ...keys]);
  };

  const clear = () => {
    setSelection([]);
  };

  return {
    toggle,
    groupToggle,
    clear,
    select,
    data: selection,
  };
};
