import { useMemo } from 'react';
import { FiltersType } from 'tg.component/translation/translationFilters/tools';
import { StateType } from 'tg.constants/translationStates';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

export type TranslationStateType = StateType | 'OUTDATED' | 'AUTO_TRANSLATED';

export type FiltersInternal = {
  filterTag?: string[];
  filterNoTag?: string[];
  filterNamespace?: string[];
  filterNoNamespace?: string[];
  filterTranslationState?: TranslationStateType[];
  filterHasScreenshot?: boolean;
  filterHasNoScreenshot?: boolean;
  filterHasUnresolvedComments?: boolean;
  filterHasComments?: boolean;

  /*
   * Specifies which languages will be considered when filtering by translation state
   *  - undefined = all but base
   *  - true = all
   *  - string = one language tag
   */
  filterTranslationLanguage?: true | string;
};

export type AddParams =
  | ['filterTag', string]
  | ['filterNoTag', string]
  | ['filterNamespace', string]
  | ['filterNoNamespace', string]
  | ['filterTranslationState', TranslationStateType]
  | ['filterHasScreenshot']
  | ['filterHasNoScreenshot']
  | ['filterHasUnresolvedComments']
  | ['filterHasComments'];

export type FilterActions = {
  addFilter: (...params: AddParams) => void;
  removeFilter: (...params: AddParams) => void;
  setFilters: (value: FiltersInternal) => void;
};

export const isFilterEmpty = (filter: FiltersType) => {
  return Object.values(filter).filter(Boolean).length === 0;
};

function remove<T extends string>(list: T[] | undefined, value: T) {
  const result = list?.filter((i) => i !== value) || [];
  return result.length ? result : undefined;
}

function add<T extends string>(list: T[] | undefined, value: T) {
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

  // adjusts filters to newly incoming languages
  // so in next render it's already correct
  function updateSelectedLanguages(newLanguages: string[] | undefined) {
    if (
      typeof filters.filterTranslationLanguage === 'string' &&
      newLanguages &&
      newLanguages.includes(filters.filterTranslationLanguage)
    ) {
      setFilters({ filterTranslationLanguage: undefined });
    }
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
        });
      case 'filterHasScreenshot':
        return setFilters({
          ...filters,
          filterHasScreenshot: true,
          filterHasNoScreenshot: undefined,
        });
      case 'filterHasNoScreenshot':
        return setFilters({
          ...filters,
          filterHasNoScreenshot: true,
          filterHasScreenshot: undefined,
        });
      case 'filterHasUnresolvedComments':
        return setFilters({
          ...filters,
          filterHasUnresolvedComments: true,
          filterHasComments: undefined,
        });
      case 'filterHasComments':
        return setFilters({
          ...filters,
          filterHasComments: true,
          filterHasUnresolvedComments: undefined,
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
      case 'filterHasScreenshot':
        return setFilters({
          ...filters,
          filterHasScreenshot: undefined,
        });
      case 'filterHasNoScreenshot':
        return setFilters({
          ...filters,
          filterHasNoScreenshot: undefined,
        });
      case 'filterHasUnresolvedComments':
        return setFilters({
          ...filters,
          filterHasUnresolvedComments: undefined,
        });
      case 'filterHasComments':
        return setFilters({
          ...filters,
          filterHasComments: undefined,
        });
    }
  }

  const filtersQuery: Partial<FiltersType> = {
    filterTag: filters.filterTag,
    filterNoTag: filters.filterNoTag,
    filterNamespace: filters.filterNamespace,
    filterNoNamespace: filters.filterNoNamespace,
    filterHasScreenshot: filters.filterHasScreenshot,
    filterHasNoScreenshot: filters.filterHasNoScreenshot,
  };

  // filters dependant on selected languages
  if (selectedLanguages?.length) {
    selectedLanguages.forEach((tag) => {
      if (filters.filterHasUnresolvedComments) {
        filtersQuery.filterHasUnresolvedCommentsInLang = add(
          filtersQuery.filterHasUnresolvedCommentsInLang,
          tag
        );
      }
      if (filters.filterHasComments) {
        filtersQuery.filterHasCommentsInLang = add(
          filtersQuery.filterHasCommentsInLang,
          tag
        );
      }
      return tag;
    });
    selectedLanguages
      .filter((tag) => {
        switch (filters.filterTranslationLanguage) {
          case undefined:
            return tag !== baseLang;
          case true:
            return true;
          default:
            return tag === filters.filterTranslationLanguage;
        }
      })
      .forEach((tag) => {
        filters.filterTranslationState?.forEach((state) => {
          if (state === 'OUTDATED') {
            filtersQuery.filterOutdatedLanguage = add(
              filtersQuery.filterOutdatedLanguage,
              tag
            );
          } else if (state === 'AUTO_TRANSLATED') {
            filtersQuery.filterAutoTranslatedInLang = add(
              filtersQuery.filterAutoTranslatedInLang,
              tag
            );
          } else {
            filtersQuery.filterState = add(
              filtersQuery.filterState,
              `${tag},${state}`
            );
          }
        });
      });
  }

  return {
    filters,
    filtersQuery,
    updateSelectedLanguages,
    addFilter,
    removeFilter,
    setFilters,
  };
}
