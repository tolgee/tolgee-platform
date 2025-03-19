import { AddParams, FiltersInternal, FiltersType } from './tools';

function remove<T extends string>(list: T[] | undefined, value: T) {
  const result = list?.filter((i) => i !== value) || [];
  return result.length ? result : undefined;
}

function add<T extends string>(list: T[] | undefined, value: T) {
  return [...(remove(list, value) || []), value];
}

type Props = {
  filters: FiltersInternal;
  setFilters: (value: FiltersInternal) => void;
  selectedLanguages?: string[];
  baseLang?: string;
};

export const useTranslationFilters = ({
  filters,
  setFilters,
  selectedLanguages,
  baseLang,
}: Props) => {
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
};
