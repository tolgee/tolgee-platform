import {
  FiltersInternal,
  FiltersType,
} from 'tg.views/projects/translations/TranslationFilters/tools';

export const getAiReviewFilterQuery = (
  filters: FiltersInternal,
  hasAiReviewFilter: boolean,
  selectedLanguages: string[] | undefined,
  baseLang: string | undefined
): Partial<FiltersType> => {
  if (!hasAiReviewFilter) {
    return {};
  }
  const languages = selectedLanguages?.filter((tag) => {
    switch (filters.filterAiReviewApplyLanguage) {
      case undefined:
        return tag !== baseLang;
      case true:
        return true;
      default:
        return tag === filters.filterAiReviewApplyLanguage;
    }
  });
  if (!languages?.length) {
    return {};
  }
  return {
    filterAiReviewOutcome: filters.filterAiReviewOutcome,
    filterAiReviewScoreBelow: filters.filterAiReviewScoreBelow,
    filterAiReviewRuleId: filters.filterAiReviewRuleId,
    filterAiReviewInLang: languages,
  };
};
