import { components } from 'tg.service/apiSchema.generated';

type ModifiedEntityModel = components['schemas']['ModifiedEntityModel'];
export type TranslationHistoryModel =
  components['schemas']['TranslationHistoryModel'];
export type ActionType = components['schemas']['ProjectActivityModel']['type'];
export type ProjectActivityModel =
  components['schemas']['ProjectActivityModel'];

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
  | 'Namespace';

export type FieldTypeEnum =
  | 'text'
  | 'translation_state'
  | 'translation_auto'
  | 'comment_state'
  | 'key_tags'
  | 'language_flag'
  | 'project_language'
  | 'namespace'
  | 'outdated';

export type FieldOptionsObj = {
  label?: string;
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

export type Reference =
  | KeyReferenceData
  | LanguageReferenceData
  | CommentReferenceData;

export type ReferenceBuilder = (data: ModifiedEntityModel) => Reference[];

export type Field = {
  name: string;
  label?: string;
  value: DiffValue<any>;
  options: FieldOptionsObj;
  languageTag?: string;
};

export type EntityOptions = {
  references?: ReferenceBuilder;
  fields: Record<string, FieldOptions>;
  description?: Reference['type'][];
  label: string;
};

export type Entity = {
  type: EntityEnum;
  options: EntityOptions;
  references: Reference[];
  fields: Field[];
};

export type Activity = {
  translationKey: string | undefined;
  type: ActivityTypeEnum;
  entities: Entity[];
  references: Reference[];
  counts: Record<string, number>;
  options: ActivityOptions;
};

export type ActivityOptions = {
  label: string;
  description?: (data: ProjectActivityModel) => string;
  entities?: Partial<Record<EntityEnum, boolean | string[]>>;
  titleReferences?: string[];
};
