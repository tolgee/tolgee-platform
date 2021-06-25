import { useSelector } from 'react-redux';
import { singleton } from 'tsyringe';
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

@singleton()
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
  otherEditionConfirm = this.createAction(
    'OTHER_EDITION_CONFIRM',
    () => {}
  ).build.on((state) => ({
    ...state,
    //@ts-ignore
    editing: { ...state.editingAfterConfirmation },
    editingAfterConfirmation: null,
  }));
  otherEditionCancel = this.createAction(
    'OTHER_EDITION_CANCEL',
    () => {}
  ).build.on((state) => ({
    ...state,
    editingAfterConfirmation: null,
  }));
  setEditingValue = this.createAction(
    'SET_EDITING_VALUE',
    (val: string) => val
    //@ts-ignore
  ).build.on((state, action) => {
    return {
      ...state,
      editing: {
        ...state.editing,
        data: { ...state.editing?.data, newValue: action.payload },
      },
    };
  });
  setTranslationEditing = this.createAction(
    'SET_TRANSLATION_EDITING',
    (data: { data: TranslationEditingType; skipConfirm?: boolean }) => data
  ).build.on((state, action) => {
    const needsConfirmation =
      !action.payload.skipConfirm &&
      state.editing &&
      state.editing.data?.initialValue !== state.editing.data?.newValue;
    return {
      ...state,
      [needsConfirmation ? 'editingAfterConfirmation' : 'editing']: {
        type: 'translation',
        data: { ...action.payload.data },
      },
    };
  });
  setKeyEditing = this.createAction(
    'SET_KEY_EDITING',
    (data: SourceEditingType) => data
  ).build.on((state, action) => {
    const needsConfirmation =
      state.editing &&
      state.editing.data?.initialValue !== state.editing.data?.newValue;
    return {
      ...state,
      [needsConfirmation ? 'editingAfterConfirmation' : 'editing']: {
        type: 'key',
        data: { ...action.payload },
      },
    };
  });

  readonly loadableDefinitions = {};

  useSelector<T>(selector: (state: TranslationsState) => T): T {
    return useSelector((state: AppState) => selector(state.translations));
  }

  get prefix(): string {
    return 'TRANSLATIONS';
  }
}
