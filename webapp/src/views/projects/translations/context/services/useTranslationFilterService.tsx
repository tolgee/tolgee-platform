import { useMemo } from 'react';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

import { FiltersInternal, FiltersType } from '../../TranslationFilters/tools';
import { useTranslationFilters } from '../../TranslationFilters/useTranslationFilters';

type Props = {
  selectedLanguages: string[] | undefined;
  baseLang: string | undefined;
};

export function useTranslationFiltersService({
  selectedLanguages,
  baseLang,
}: Props) {
  const [_filters, _setFilters] = useUrlSearchState('filters', {
    defaultVal: JSON.stringify({}),
  });

  const filters = useMemo(
    () => (_filters ? JSON.parse(_filters as string) : {}) as FiltersType,
    [_filters]
  ) as FiltersInternal;

  function setFilters(value: FiltersInternal) {
    _setFilters(JSON.stringify(value));
  }

  return useTranslationFilters({
    filters,
    setFilters,
    selectedLanguages,
    baseLang,
  });
}
