import { useSelector } from 'react-redux';
import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../AbstractLoadableActions';
import { AppState } from '../index';

export type TranslationEditingType = {
  key: string;
  languageAbbreviation: string;
  initialValue: string;
  newValue: string;
} | null;
export type SourceEditingType = { initialValue: string; newValue: string };

export class TranslationsState extends StateWithLoadables<TranslationActions> {
  selectedLanguages: string[] | null = null;
  editing: {
    type: 'key' | 'translation';
    data: TranslationEditingType | SourceEditingType | null;
  } | null = null;
  editingAfterConfirmation: {
    type: 'key' | 'translation';
    data: TranslationEditingType | SourceEditingType | null;
  } | null = null;
}

export class TranslationActions extends AbstractLoadableActions<TranslationsState> {
  constructor() {
    super(new TranslationsState());
  }

  select = this.createAction(
    'SELECT_LANGUAGES',
    (langs: string[] | null) => langs
  ).build.on(
    (state, action) =>
      <TranslationsState>{ ...state, selectedLanguages: action.payload }
  );

  readonly loadableDefinitions = {};

  useSelector<T>(selector: (state: TranslationsState) => T): T {
    return useSelector((state: AppState) => selector(state.translations));
  }

  get prefix(): string {
    return 'TRANSLATIONS';
  }
}

export const translationActions = new TranslationActions();
