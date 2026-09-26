import { StateType } from 'tg.constants/translationStates';
import { operations, components } from 'tg.service/apiSchema.generated';
import { TaskType } from 'tg.service/apiSchemaTypes';

export type FiltersType = operations['getTranslations']['parameters']['query'];

export type LanguageModel = components['schemas']['LanguageModel'];

type QaCheckType = components['schemas']['QaIssueModel']['type'];

export type TranslationStateType = StateType | 'OUTDATED' | 'AUTO_TRANSLATED';

export type TaskStatusFilter =
  | 'NOT_IN_OPEN_TASK'
  | 'HAS_BEEN_IN_TASK'
  | 'NEVER_IN_TASK';

export type FiltersInternal = {
  filterTag?: string[];
  filterNoTag?: string[];
  filterNamespace?: string[];
  filterNoNamespace?: string[];
  filterTranslationState?: TranslationStateType[];
  filterHasScreenshot?: boolean;
  filterHasNoScreenshot?: boolean;
  filterHasDescription?: boolean;
  filterHasNoDescription?: boolean;
  filterHasUnresolvedComments?: boolean;
  filterHasComments?: boolean;
  filterQaCheckTypes?: QaCheckType[];
  filterQaChecksStale?: boolean;
  filterLabel?: string[];
  filterHasSuggestions?: boolean;
  filterHasNoSuggestions?: boolean;
  filterTaskStatus?: TaskStatusFilter;
  filterTaskType?: TaskType[];
  filterDeletedByUserId?: number[];

  /*
   * Specifies which languages will be considered when filtering by translation state
   *  - undefined = all but base
   *  - true = all
   *  - string = one language tag
   */
  filterTranslationLanguage?: true | string;
  // same for suggestions and tasks
  filterSuggestionLanguage?: true | string;
  filterTaskLanguage?: true | string;

  /*
   * this one differs from the others above
   *
   * Specifies which languages will be considered when filtering by the QA check type:
   *  - undefined = all languages (default)
   *  - false = all but base
   *  - string = one language tag
   */
  filterQaCheckTypeLanguage?: false | string;
};

export type AddParams =
  | ['filterTag', string]
  | ['filterNoTag', string]
  | ['filterNamespace', string]
  | ['filterNoNamespace', string]
  | ['filterTranslationState', TranslationStateType]
  | ['filterHasScreenshot']
  | ['filterHasNoScreenshot']
  | ['filterHasDescription']
  | ['filterHasNoDescription']
  | ['filterHasUnresolvedComments']
  | ['filterHasComments']
  | ['filterQaCheckTypes', QaCheckType]
  | ['filterQaChecksStale']
  | ['filterLabel', string]
  | ['filterHasSuggestions']
  | ['filterHasNoSuggestions']
  | ['filterDeletedByUserId', number];

export type FilterActions = {
  addFilter: (...params: AddParams) => void;
  removeFilter: (...params: AddParams) => void;
  setFilters: (value: FiltersInternal) => void;
};

export type FilterOptions = {
  keyRelatedOnly?: boolean;
  showDeletedBy?: boolean;
  taskCreation?: boolean;
  clearedFilters?: FiltersInternal;
  pinnedTaskType?: TaskType;
};
