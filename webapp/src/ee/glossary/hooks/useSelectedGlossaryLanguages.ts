import { useMemo, useState } from 'react';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';

export const useSelectedGlossaryLanguages = () => {
  const glossary = useGlossary();

  const [selectedFor, setSelectedFor] = useState<number | undefined>(undefined);
  const [selected, setSelected] = useState<string[] | undefined>(undefined);

  const selectedWithBaseLanguage = useMemo(() => {
    if (selectedFor !== glossary.id) {
      return undefined;
    }
    if (selected === undefined) {
      return undefined;
    }
    return [glossary?.baseLanguageTag || '', ...(selected ?? [])];
  }, [selectedFor, selected, glossary]);

  const update = (languages: string[]) => {
    setSelectedFor(glossary.id);
    setSelected(languages.filter((l) => l !== glossary.baseLanguageTag));
  };

  return [selectedWithBaseLanguage, update] as const;
};
