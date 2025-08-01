import { StateType } from 'tg.constants/translationStates';
import { operations, components } from 'tg.service/apiSchema.generated';

export type FiltersType = operations['getTranslations']['parameters']['query'];

export type LanguageModel = components['schemas']['LanguageModel'];

export const isFilterEmpty = (filter: FiltersType) => {
  return Object.values(filter).filter(Boolean).length === 0;
};

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
  filterLabel?: string[];
  filterHasSuggestions?: boolean;
  filterHasNoSuggestions?: boolean;

  /*
   * Specifies which languages will be considered when filtering by translation state
   *  - undefined = all but base
   *  - true = all
   *  - string = one language tag
   */
  filterTranslationLanguage?: true | string;
  // same for suggestions
  filterSuggestionLanguage?: true | string;
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
  | ['filterHasComments']
  | ['filterLabel', string]
  | ['filterHasSuggestions']
  | ['filterHasNoSuggestions'];

export type FilterActions = {
  addFilter: (...params: AddParams) => void;
  removeFilter: (...params: AddParams) => void;
  setFilters: (value: FiltersInternal) => void;
};

export type FilterOptions = {
  keyRelatedOnly?: boolean;
};
