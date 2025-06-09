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

export type GlossaryTermHighlightModel =
  components['schemas']['GlossaryTermHighlightModel'];

export type GlossaryTermModel = components['schemas']['GlossaryTermModel'];

export type GlossaryTermHighlightsProps = {
  text: string | null | undefined;
  languageTag: string;
  enabled?: boolean;
};

export type GlossaryTermPreviewProps = {
  term: GlossaryTermModel;
  languageTag: string;
  targetLanguageTag?: string;
  appendValue?: (val: string) => void;
  standalone?: boolean;
  slim?: boolean;
};
