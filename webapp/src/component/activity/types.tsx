import { TranslateParams } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type ModifiedEntityModel = components['schemas']['ModifiedEntityModel'];
export type TranslationHistoryModel =
  components['schemas']['TranslationHistoryModel'];
export type ActionType = components['schemas']['ProjectActivityModel']['type'];
export type ProjectActivityModel =
  components['schemas']['ProjectActivityModel'];

type TaskModel = components['schemas']['TaskModel'];

export type DiffValue<T = string> = {
  old?: T;
  new?: T;
};

export type ActivityTypeEnum =
  | ActionType
  | 'TRANSLATION_HISTORY_ADD'
  | 'TRANSLATION_HISTORY_MODIFY'
  | 'TRANSLATION_HISTORY_DELETE';

export type ChangeTypeOfKeys<
  T extends Record<string, unknown>,
  Keys extends keyof T,
  NewType
> = {
  [key in keyof T]: key extends Keys ? NewType : T[key];
};

export type ActivityModel = ChangeTypeOfKeys<
  ProjectActivityModel,
  'type',
  ActivityTypeEnum
>;

export type EntityEnum =
  | 'Translation'
  | 'Language'
  | 'Key'
  | 'KeyMeta'
  | 'TranslationComment'
  | 'Screenshot'
  | 'Project'
  | 'Namespace'
  | 'Params'
  | 'ContentDeliveryConfig'
  | 'WebhookConfig'
  | 'ContentStorage'
  | 'Task';

export type FieldTypeEnum =
  | 'text'
  | 'translation_state'
  | 'translation_auto'
  | 'default_namespace'
  | 'comment_state'
  | 'key_tags'
  | 'language_flag'
  | 'project_language'
  | 'namespace'
  | 'outdated'
  | 'batch_language_ids'
  | 'batch_language_id'
  | 'batch_key_tag_list'
  | 'batch_namespace'
  | 'batch_translation_state'
  | 'batch_boolean'
  | 'state_array'
  | 'language_tags'
  | 'date'
  | 'task_state'
  | 'task_type';

export type FieldOptionsObj = {
  label?: (params?: TranslateParams) => React.ReactElement;
  type?: FieldTypeEnum;
  compute?: (data: any) => any;
};

export type FieldOptions = boolean | FieldOptionsObj;

export type LanguageReferenceType = {
  tag: string;
  name: string;
  flagEmoji: string;
};

export type KeyReferenceData = {
  type: 'key';
  keyName: string;
  namespace: string;
  languages?: LanguageReferenceType[];
  id?: number;
  exists?: boolean;
};

export type LanguageReferenceData = {
  type: 'language';
  language: LanguageReferenceType;
};

export type CommentReferenceData = {
  type: 'comment';
  text: string;
};

export type ContentDeliveryConfigReferenceData = {
  type: 'content_delivery_config';
  name: string;
};

export type ContentStorageReferenceData = {
  type: 'content_storage';
  name: string;
};

export type WebhookConfigReferenceData = {
  type: 'webhook_config';
  url: string;
};

export type TaskReferenceData = {
  type: 'task';
  name: string;
  taskType: TaskModel['type'];
  number: number;
};

export type Reference =
  | KeyReferenceData
  | LanguageReferenceData
  | CommentReferenceData
  | ContentDeliveryConfigReferenceData
  | ContentStorageReferenceData
  | WebhookConfigReferenceData
  | TaskReferenceData;

export type ReferenceBuilder = (
  data: ModifiedEntityModel
) => Reference[] | undefined;

export type Field = {
  name: string;
  label?: (params?: TranslateParams) => React.ReactElement;
  value: DiffValue<any>;
  options: FieldOptionsObj;
  languageTag?: string;
};

export type EntityOptions = {
  references?: ReferenceBuilder;
  fields: Record<string, FieldOptions>;
  description?: Reference['type'][];
  label: (params?: TranslateParams) => React.ReactElement;
};

export type Entity = {
  type: EntityEnum;
  options: EntityOptions;
  references: Reference[];
  fields: Field[];
};

export type Activity = {
  translation: ((params?: TranslateParams) => React.ReactElement) | undefined;
  type: ActivityTypeEnum;
  entities: Entity[];
  references: Reference[];
  counts: Record<string, number>;
  options: ActivityOptions;
};

export type ActivityOptions = {
  label: (params?: TranslateParams) => React.ReactElement;
  description?: (data: ProjectActivityModel) => string;
  entities?: Partial<Record<EntityEnum, boolean | string[]>>;
  titleReferences?: string[];
  compactFieldCount?: number;
};
