import { useEffect, useMemo, useState } from 'react';

import { components } from 'tg.service/apiSchema.generated';
import { TranslationStateType } from 'tg.translationTools/useStateTranslation';
import { TaskType } from 'tg.service/apiSchemaTypes';
import {
  FiltersInternal,
  FiltersType,
} from 'tg.views/projects/translations/TranslationFilters/tools';
import { useTranslationFilters } from 'tg.views/projects/translations/TranslationFilters/useTranslationFilters';

type LanguageModel = components['schemas']['LanguageModel'];

const DEFAULT_TASK_STATUS = 'NOT_IN_OPEN_TASK';

type Props = {
  allLanguages: LanguageModel[];
  initialLanguages?: number[];
  keysPreselected: boolean;
};

export function useTaskCreationFilters({
  allLanguages,
  initialLanguages,
  keysPreselected,
}: Props) {
  const defaultFilters: FiltersInternal = keysPreselected
    ? {}
    : { filterTaskStatus: DEFAULT_TASK_STATUS };

  const [filters, storeFilters] = useState<FiltersInternal>(defaultFilters);

  function setFilters(value: FiltersInternal) {
    storeFilters({ ...defaultFilters, ...value });
  }

  const [stateFilters, setStateFilters] = useState<TranslationStateType[]>();
  const [languages, setLanguages] = useState(initialLanguages ?? []);

  const selectedLanguageTags = useMemo(
    () =>
      languages
        .map((id) => allLanguages.find((l) => l.id === id)?.tag)
        .filter((tag): tag is string => Boolean(tag)),
    [languages, allLanguages]
  );

  const { filtersQuery, ...actions } = useTranslationFilters({
    filters,
    setFilters,
    selectedLanguages: selectedLanguageTags,
  });

  const { clearFiltersForRemovedLanguages } = actions;
  useEffect(() => {
    clearFiltersForRemovedLanguages(selectedLanguageTags);
  }, [selectedLanguageTags]);

  return {
    filters,
    defaultFilters,
    setFilters,
    actions,
    filtersQuery,
    languages,
    setLanguages,
    stateFilters,
    setStateFilters,
  };
}

/**
 * The task status filter is applied per target language by TranslationScopeFilters, which runs once
 * per language. `select-all` builds a single key set shared by every target language, so leaving the
 * status in that query drops a key from all of them as soon as one language has it tasked.
 */
export function omitTaskScopeQuery<T extends FiltersType>(query: T): T {
  const result = { ...query };
  delete result.filterHasBeenInTaskInLang;
  delete result.filterNeverInTaskInLang;
  delete result.filterNotInOpenTaskInLang;
  delete result.filterTaskType;
  return result;
}

/**
 * Task creation excludes keys already in an open task of the type being created, so the scope sent
 * to the server must always cover that type — otherwise the preview counts keys the creation drops.
 */
export function taskScopeFiltersQuery(
  filters: FiltersInternal,
  createdType: TaskType
) {
  const selectedTypes = filters.filterTaskType ?? [];
  return {
    filterNeverInTask: filters.filterTaskStatus === 'NEVER_IN_TASK',
    filterHasBeenInTask: filters.filterTaskStatus === 'HAS_BEEN_IN_TASK',
    filterNotInOpenTask: filters.filterTaskStatus === 'NOT_IN_OPEN_TASK',
    filterTaskType: selectedTypes.includes(createdType)
      ? selectedTypes
      : [...selectedTypes, createdType],
  };
}
