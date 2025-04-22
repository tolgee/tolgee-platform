import {
  HierarchyItem,
  LanguageModel,
  PermissionAdvancedState,
} from 'tg.component/PermissionsSettings/types';
import { components } from 'tg.service/apiSchema.generated';

export type PermissionsAdvancedEeProps = {
  dependencies: HierarchyItem;
  state: PermissionAdvancedState;
  onChange: (value: PermissionAdvancedState) => void;
  allLangs?: LanguageModel[];
};

export type BillingMenuItemsProps = {
  onClose: () => void;
};

export type TaskModel = components['schemas']['TaskModel'];

export type TaskReferenceData = {
  type: 'task';
  name: string;
  taskType: TaskModel['type'];
  number: number;
};

export type TaskReferenceProps = {
  data: TaskReferenceData;
};

export type TranslationTaskIndicatorProps = {
  task?: components['schemas']['KeyTaskViewModel'];
};

export type PrefilterTaskProps = {
  taskNumber: number;
};

export type GlossaryTermHighlightDto =
  components['schemas']['GlossaryTermHighlightDto'];

export type GlossaryTermWithTranslationsModel =
  components['schemas']['GlossaryTermWithTranslationsModel'];

export type GlossaryTermHighlightsProps = {
  text: string;
  languageTag: string;
  enabled?: boolean;
};

export type GlossaryTooltipProps = {
  term: GlossaryTermWithTranslationsModel;
  languageTag: string;
  targetLanguageTag?: string;
};
