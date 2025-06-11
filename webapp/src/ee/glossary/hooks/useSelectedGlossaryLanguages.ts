import { useMemo, useState } from 'react';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';

export const useSelectedGlossaryLanguages = () => {
  const glossary = useGlossary();

  const [selected, setSelected] = useState<string[] | undefined>(undefined);

  const selectedWithBaseLanguage = useMemo(() => {
    if (selected === undefined) {
      return undefined;
    }
    return [glossary?.baseLanguageTag || '', ...(selected ?? [])];
  }, [selected, glossary]);

  const update = (languages: string[]) => {
    setSelected(languages.filter((l) => l !== glossary.baseLanguageTag));
  };

  return [selectedWithBaseLanguage, update] as const;
};
