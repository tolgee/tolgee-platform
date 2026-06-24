import { exhaustiveMatchingGuard } from 'tg.fixtures/exhaustiveMatchingGuard';
import { AddParams, FiltersInternal, FiltersType } from './tools';

function remove<T extends string | number>(list: T[] | undefined, value: T) {
  const result = list?.filter((i) => i !== value) || [];
  return result.length ? result : undefined;
}

function add<T extends string | number>(list: T[] | undefined, value: T) {
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
      case 'filterHasDescription':
        return setFilters({
          ...filters,
          filterHasDescription: true,
          filterHasNoDescription: undefined,
        });
      case 'filterHasNoDescription':
        return setFilters({
          ...filters,
          filterHasNoDescription: true,
          filterHasDescription: undefined,
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
      case 'filterQaCheckTypes':
        return setFilters({
          ...filters,
          filterQaCheckTypes: add(filters.filterQaCheckTypes, value),
        });
      case 'filterQaChecksStale':
        return setFilters({
          ...filters,
          filterQaChecksStale: true,
        });
      case 'filterLabel':
        return setFilters({
          ...filters,
          filterLabel: add(filters.filterLabel, value),
        });
      case 'filterHasSuggestions':
        return setFilters({
          ...filters,
          filterHasSuggestions: true,
          filterHasNoSuggestions: undefined,
        });
      case 'filterHasNoSuggestions':
        return setFilters({
          ...filters,
          filterHasNoSuggestions: true,
          filterHasSuggestions: undefined,
        });
      case 'filterDeletedByUserId':
        return setFilters({
          ...filters,
          filterDeletedByUserId: add(filters.filterDeletedByUserId, value),
        });
      default:
        exhaustiveMatchingGuard(type);
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
      case 'filterHasDescription':
        return setFilters({
          ...filters,
          filterHasDescription: undefined,
        });
      case 'filterHasNoDescription':
        return setFilters({
          ...filters,
          filterHasNoDescription: undefined,
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
      case 'filterQaCheckTypes':
        return setFilters({
          ...filters,
          filterQaCheckTypes: remove(filters.filterQaCheckTypes, value),
        });
      case 'filterQaChecksStale':
        return setFilters({
          ...filters,
          filterQaChecksStale: undefined,
        });
      case 'filterLabel':
        return setFilters({
          ...filters,
          filterLabel: remove(filters.filterLabel, value),
        });
      case 'filterHasSuggestions':
        return setFilters({
          ...filters,
          filterHasSuggestions: undefined,
        });
      case 'filterHasNoSuggestions':
        return setFilters({
          ...filters,
          filterHasNoSuggestions: undefined,
        });
      case 'filterDeletedByUserId':
        return setFilters({
          ...filters,
          filterDeletedByUserId: remove(filters.filterDeletedByUserId, value),
        });
      default:
        exhaustiveMatchingGuard(type);
    }
  }

  const filtersQuery: Partial<FiltersType> = {
    filterTag: filters.filterTag,
    filterNoTag: filters.filterNoTag,
    filterNamespace: filters.filterNamespace,
    filterNoNamespace: filters.filterNoNamespace,
    filterHasScreenshot: filters.filterHasScreenshot,
    filterHasNoScreenshot: filters.filterHasNoScreenshot,
    filterHasDescription: filters.filterHasDescription,
    filterHasNoDescription: filters.filterHasNoDescription,
    filterDeletedByUserId: filters.filterDeletedByUserId,
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
    });

    selectedLanguages
      .filter((tag) => {
        switch (filters.filterQaCheckTypeLanguage) {
          case undefined:
            return true;
          case false:
            return tag !== baseLang;
          default:
            return tag === filters.filterQaCheckTypeLanguage;
        }
      })
      .forEach((tag) => {
        if (filters.filterQaChecksStale) {
          filtersQuery.filterQaChecksStaleInLang = add(
            filtersQuery.filterQaChecksStaleInLang,
            tag
          );
        }
        filters.filterQaCheckTypes?.forEach((type) => {
          filtersQuery.filterQaCheckType = add(
            filtersQuery.filterQaCheckType,
            `${tag},${type}`
          );
        });
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
        filters.filterLabel?.forEach((id) => {
          filtersQuery.filterLabel = add(
            filtersQuery.filterLabel,
            `${tag},${id.toString()}`
          );
        });
      });

    selectedLanguages
      .filter((tag) => {
        switch (filters.filterSuggestionLanguage) {
          case undefined:
            return tag !== baseLang;
          case true:
            return true;
          default:
            return tag === filters.filterSuggestionLanguage;
        }
      })
      .forEach((tag) => {
        if (filters.filterHasSuggestions) {
          filtersQuery.filterHasSuggestionsInLang = add(
            filtersQuery.filterHasSuggestionsInLang,
            tag
          );
        }
        if (filters.filterHasNoSuggestions) {
          filtersQuery.filterHasNoSuggestionsInLang = add(
            filtersQuery.filterHasNoSuggestionsInLang,
            tag
          );
        }
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
