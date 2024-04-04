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

  const clear = () => {
    setSelection([]);
  };

  return {
    toggle,
    clear,
    select,
    data: selection,
  };
};
