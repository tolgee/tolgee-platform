import { useMemo } from 'react';
import { FiltersType } from 'tg.component/translation/translationFilters/tools';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

export type FiltersInternal = {
  filterTag?: string[];
  filterNoTag?: string[];
  filterNamespace?: string[];
  filterNoNamespace?: string[];
  filterTranslationState?: string[];
  filterNoTranslationState?: string[];
  filterTranslationStateApplyBaseLang?: boolean;
};

export type AddParams =
  | ['filterTag', string]
  | ['filterNoTag', string]
  | ['filterNamespace', string]
  | ['filterNoNamespace', string]
  | ['filterTranslationState', string]
  | ['filterNoTranslationState', string];

export type FilterActions = {
  addFilter: (...params: AddParams) => void;
  removeFilter: (...params: AddParams) => void;
  setFilters: (value: FiltersInternal) => void;
};

export const isFilterEmpty = (filter: FiltersType) => {
  return Object.values(filter).filter(Boolean).length === 0;
};

function remove(list: string[] | undefined, value: string) {
  const result = list?.filter((i) => i !== value) || [];
  return result.length ? result : undefined;
}

function add(list: string[] | undefined, value: string) {
  return [...(remove(list, value) || []), value];
}

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

  function addFilter(...params: AddParams) {
    const [type, value] = params;
    switch (type) {
      case 'filterTag':
        return setFilters({
          ...filters,
          filterTag: add(filters.filterTag, value),
          filterNoTag: remove(filters.filterNoTag, value),
        });
      case 'filterNoTag':
        return setFilters({
          ...filters,
          filterNoTag: add(filters.filterNoTag, value),
          filterTag: remove(filters.filterTag, value),
        });
      case 'filterNamespace':
        return setFilters({
          ...filters,
          filterNamespace: add(filters.filterNamespace, value),
          filterNoNamespace: remove(filters.filterNoNamespace, value),
        });
      case 'filterNoNamespace':
        return setFilters({
          ...filters,
          filterNoNamespace: add(filters.filterNoNamespace, value),
          filterNamespace: remove(filters.filterNamespace, value),
        });
      case 'filterTranslationState':
        return setFilters({
          ...filters,
          filterTranslationState: add(filters.filterTranslationState, value),
          filterNoTranslationState: remove(
            filters.filterNoTranslationState,
            value
          ),
        });
      case 'filterNoTranslationState':
        return setFilters({
          ...filters,
          filterNoTranslationState: add(
            filters.filterNoTranslationState,
            value
          ),
          filterTranslationState: remove(filters.filterTranslationState, value),
        });
    }
  }

  function removeFilter(...params: AddParams) {
    const [type, value] = params;
    switch (type) {
      case 'filterTag':
        return setFilters({
          ...filters,
          filterTag: remove(filters.filterTag, value),
        });
      case 'filterNoTag':
        return setFilters({
          ...filters,
          filterNoTag: remove(filters.filterNoTag, value),
        });
      case 'filterNamespace':
        return setFilters({
          ...filters,
          filterNamespace: remove(filters.filterNamespace, value),
        });
      case 'filterNoNamespace':
        return setFilters({
          ...filters,
          filterNoNamespace: remove(filters.filterNoNamespace, value),
        });
      case 'filterTranslationState':
        return setFilters({
          ...filters,
          filterTranslationState: remove(filters.filterTranslationState, value),
        });
      case 'filterNoTranslationState':
        return setFilters({
          ...filters,
          filterNoTranslationState: remove(
            filters.filterNoTranslationState,
            value
          ),
        });
    }
  }

  const filtersQuery: Partial<FiltersType> = {
    filterTag: filters.filterTag,
    filterNoTag: filters.filterNoTag,
    filterNamespace: filters.filterNamespace,
    filterNoNamespace: filters.filterNoNamespace,
  };

  if (filters.filterTranslationState?.length && selectedLanguages?.length) {
    filtersQuery.filterState = [];
    selectedLanguages
      .filter((tag) => {
        return filters.filterTranslationStateApplyBaseLang || tag !== baseLang;
      })
      .forEach((tag) => {
        filters.filterTranslationState?.forEach((state) => {
          filtersQuery.filterState?.push(`${tag},${state}`);
        });
      });
  }

  return { filters, filtersQuery, addFilter, removeFilter, setFilters };
}
