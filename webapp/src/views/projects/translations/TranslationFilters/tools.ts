import { StateType } from 'tg.constants/translationStates';
import { operations, components } from 'tg.service/apiSchema.generated';
import { TaskType } from 'tg.service/apiSchemaTypes';

export type FiltersType = operations['getTranslations']['parameters']['query'];

export type LanguageModel = components['schemas']['LanguageModel'];

type QaCheckType = components['schemas']['QaIssueModel']['type'];

export type TranslationStateType = StateType | 'OUTDATED' | 'AUTO_TRANSLATED';

export type AiReviewFilterOutcome = NonNullable<
  FiltersType['filterAiReviewStatus']
>[number];

export type AiReviewScoreFilterMode = NonNullable<
  FiltersType['filterAiReviewScoreMode']
>;

export type TaskStatusFilter =
  | 'IN_OPEN_TASK'
  | 'NOT_IN_OPEN_TASK'
  | 'HAS_BEEN_IN_TASK'
  | 'NEVER_IN_TASK';

export const TASK_STATUSES: TaskStatusFilter[] = [
  'IN_OPEN_TASK',
  'NOT_IN_OPEN_TASK',
  'HAS_BEEN_IN_TASK',
  'NEVER_IN_TASK',
];

/** One condition per task type; a type with no entry is unconstrained ("Any"). */
export type TaskStatusByType = Partial<Record<TaskType, TaskStatusFilter>>;

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
  filterIsPlural?: boolean;
  filterIsNotPlural?: boolean;
  filterHasUnresolvedComments?: boolean;
  filterHasComments?: boolean;
  filterQaCheckTypes?: QaCheckType[];
  filterQaChecksStale?: boolean;
  filterAiReviewOutcomes?: AiReviewFilterOutcome[];
  filterAiReviewScoreMode?: AiReviewScoreFilterMode;
  filterAiReviewScore?: number;
  filterLabel?: string[];
  filterHasSuggestions?: boolean;
  filterHasNoSuggestions?: boolean;
  filterTaskStatus?: TaskStatusByType;
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

  filterQaCheckTypeLanguage?: LanguageScope;
  filterAiReviewOutcomeLanguage?: LanguageScope;
};

/** Unlike above: undefined = all languages, false = all but base, string = one language tag. */
type LanguageScope = false | string;

export function aiReviewScoreFilterActive(value: FiltersInternal) {
  return (
    value.filterAiReviewScoreMode !== undefined &&
    value.filterAiReviewScore !== undefined
  );
}

export function withoutAiReviewScoreFilter(
  value: FiltersInternal
): FiltersInternal {
  return {
    ...value,
    filterAiReviewScoreMode: undefined,
    filterAiReviewScore: undefined,
  };
}

export function withoutNotReviewedYet(value: FiltersInternal): FiltersInternal {
  const outcomes = value.filterAiReviewOutcomes?.filter(
    (outcome) => outcome !== 'NOT_REVIEWED_YET'
  );
  return {
    ...value,
    filterAiReviewOutcomes: outcomes?.length ? outcomes : undefined,
  };
}

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
  | ['filterIsPlural']
  | ['filterIsNotPlural']
  | ['filterHasUnresolvedComments']
  | ['filterHasComments']
  | ['filterQaCheckTypes', QaCheckType]
  | ['filterQaChecksStale']
  | ['filterAiReviewOutcomes', AiReviewFilterOutcome]
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
  /** The view is already scoped to one task, which leaves nothing for the task filter to say. */
  taskPrefiltered?: boolean;
  taskCreation?: boolean;
  pinnedTaskType?: TaskType;
};
