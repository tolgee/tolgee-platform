import { useEffect, useMemo, useState } from 'react';

import { components } from 'tg.service/apiSchema.generated';
import { TranslationStateType } from 'tg.translationTools/useStateTranslation';
import { TaskType } from 'tg.service/apiSchemaTypes';

import {
  FiltersInternal,
  FiltersType,
  TaskStatusFilter,
} from 'tg.views/projects/translations/TranslationFilters/tools';
import { useTranslationFilters } from 'tg.views/projects/translations/TranslationFilters/useTranslationFilters';

type LanguageModel = components['schemas']['LanguageModel'];

export const DEFAULT_PINNED_STATUS: TaskStatusFilter = 'NOT_IN_OPEN_TASK';

/**
 * Task creation drops keys already in an open task of the type being created, so the type under
 * construction may only carry conditions that exclude those keys — "Any" included would be a lie.
 */
export const PINNED_TYPE_STATUSES: TaskStatusFilter[] = [
  'NOT_IN_OPEN_TASK',
  'NEVER_IN_TASK',
];

/**
 * The condition in force for the type being created. A condition picked while that type was not
 * the one being created survives the form switching onto it, and the submenu then offers nothing
 * matching it — so it must not reach the scope query either.
 */
export function pinnedTaskStatus(status: TaskStatusFilter | undefined) {
  if (status !== undefined && PINNED_TYPE_STATUSES.includes(status)) {
    return status;
  }
  return DEFAULT_PINNED_STATUS;
}

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
  const [filters, storeFilters] = useState<FiltersInternal>({});

  function setFilters(value: FiltersInternal) {
    storeFilters(value);
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
  delete result.filterTaskInLang;
  return result;
}

/**
 * Task creation excludes keys already in an open task of the type being created, so the scope sent
 * to the server must always cover that type — otherwise the preview counts keys the creation drops.
 */
export function taskScopeFiltersQuery(
  filters: FiltersInternal,
  createdType: TaskType,
  /**
   * False for the flows that open with keys already picked. They show no condition for the created
   * type, so forcing one would drop hand-picked keys and, by making the two scope counts agree,
   * also suppress the warning that is the only thing naming the drop.
   */
  constrainCreatedType = true
) {
  const byType = { ...(filters.filterTaskStatus ?? {}) };
  if (constrainCreatedType) {
    byType[createdType] = pinnedTaskStatus(byType[createdType]);
  }
  return {
    filterTaskInStatus: Object.entries(byType).map(
      ([type, status]) => `${type},${status}`
    ),
  };
}
