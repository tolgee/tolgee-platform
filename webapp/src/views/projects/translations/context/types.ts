import { components, operations } from 'tg.service/apiSchema.generated';
import { StateInType } from 'tg.constants/translationStates';
import { TolgeeFormat } from '@tginternal/editor';

type TranslationViewModel = components['schemas']['TranslationViewModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];
type TranslationsQueryType =
  operations['getTranslations']['parameters']['query'];

export type DeletableKeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'] & { deleted?: boolean };

type ScreenshotModel = components['schemas']['ScreenshotModel'];
type LanguageModel = components['schemas']['LanguageModel'];

export interface CellPosition {
  keyId: number;
  language: string | undefined;
}

export type KeyElement = CellPosition & {
  ref: HTMLElement;
};

export type ScrollToElement = CellPosition & {
  options?: ScrollIntoViewOptions;
};

export type ViewMode = 'LIST' | 'TABLE';

export type AddTranslation = KeyWithTranslationsModel;

export type UpdateTranslation = {
  keyId: number;
  lang: string;
  data: ValueUpdate<Partial<TranslationViewModel>>;
};

export type RemoveTag = {
  keyId: number;
  tagId: number;
};

export type AddTag = {
  keyId: number;
  name: string;
  onSuccess?: () => void;
};

export type LabelOperation = {
  keyId: number;
  language: LanguageModel;
  labelId: number;
};

export type AddLabel = LabelOperation & {
  translationId?: number;
};

export type RemoveLabel = LabelOperation & {
  translationId: number;
};

export type AfterCommand = 'EDIT_NEXT';

export type ChangeValue = {
  after?: AfterCommand;
  preventTaskResolution?: boolean;
  suggestionOnly?: boolean;
  onSuccess?: () => void;
};

export type Filters = Pick<
  TranslationsQueryType,
  | 'filterHasNoScreenshot'
  | 'filterHasScreenshot'
  | 'filterTranslatedAny'
  | 'filterUntranslatedAny'
  | 'filterTranslatedInLang'
  | 'filterUntranslatedInLang'
  | 'filterState'
  | 'filterTag'
>;

export type SetTranslationState = {
  keyId: number;
  translationId: number;
  language: string;
  state: StateInType;
};

export type SetTaskTranslationState = {
  done: boolean;
  taskNumber: number;
  keyId: number;
};

export type ChangeScreenshotNum = {
  keyId: number;
  screenshots: ValueUpdate<ScreenshotModel[]>;
};

export type Direction = 'DOWN';

export type SetEdit = CellPosition & {
  value: string;
};

export type EditorProps = CellPosition & {
  mode?: EditMode;
  activeVariant?: string;
};

export type PluralVariants = Record<string, string | undefined>;

export type EditPlural = TolgeeFormat | null;

export type Edit = CellPosition & {
  value: TolgeeFormat;
  changed?: boolean;
  mode?: EditMode;
  activeVariant: string;
};

export type EditMode = 'general' | 'advanced' | 'context' | 'comments';

export type ValueUpdate<Data, Update = Data> =
  | Update
  | ((data: Data) => Update);

export type KeyUpdateData = {
  keyId: number;
  value: ValueUpdate<
    DeletableKeyWithTranslationsModelType,
    Partial<DeletableKeyWithTranslationsModelType>
  >;
};
