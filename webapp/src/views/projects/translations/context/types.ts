import { components, operations } from 'tg.service/apiSchema.generated';
import { StateType } from 'tg.constants/translationStates';

type TranslationViewModel = components['schemas']['TranslationViewModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];
type TranslationsQueryType =
  operations['getTranslations']['parameters']['query'];

export type DeletableKeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'] & { deleted?: boolean };

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
  data: Partial<TranslationViewModel>;
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

export type AfterCommand = 'EDIT_NEXT';

export type ChangeValue = {
  after?: AfterCommand;
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
  state: StateType;
};

export type ChangeScreenshotNum = {
  keyId: number;
  screenshotCount: number | undefined;
};

export type Direction = 'DOWN';

export type SetEdit = CellPosition & {
  value: string;
};

export type Edit = CellPosition & {
  value?: string;
  changed?: boolean;
  mode: EditMode;
};

export type EditMode = 'editor' | 'comments';

export type KeyUpdateData = {
  keyId: number;
  value: Partial<DeletableKeyWithTranslationsModelType>;
};
