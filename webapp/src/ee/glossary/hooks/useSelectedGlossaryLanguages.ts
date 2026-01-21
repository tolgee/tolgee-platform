import { useMemo, useState } from 'react';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { glossaryPreferencesService } from '../services/GlossaryPreferencesService';

export const useSelectedGlossaryLanguages = () => {
  const glossary = useGlossary();

  const [selectedFor, setSelectedFor] = useState<number | undefined>(undefined);
  const [selected, setSelected] = useState<string[] | undefined>(undefined);

  const defaultValue = useMemo(() => {
    let saved = glossaryPreferencesService.getForGlossary(glossary.id);
    if (saved !== undefined && !saved.includes(glossary.baseLanguageTag)) {
      saved = [glossary.baseLanguageTag, ...saved];
    }
    return saved;
  }, [glossary]);

  const selectedWithBaseLanguage = useMemo(() => {
    if (selectedFor !== glossary.id) {
      return defaultValue;
    }
    if (selected === undefined) {
      return defaultValue;
    }
    return [glossary?.baseLanguageTag || '', ...(selected ?? [])];
  }, [selectedFor, selected, glossary, defaultValue]);

  const update = (languages: string[]) => {
    const withoutBase = languages.filter((l) => l !== glossary.baseLanguageTag);
    setSelectedFor(glossary.id);
    setSelected(withoutBase);
    glossaryPreferencesService.setForGlossary(glossary.id, withoutBase);
  };

  return [selectedWithBaseLanguage, update] as const;
};
