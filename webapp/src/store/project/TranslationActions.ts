import {container, singleton} from 'tsyringe';
import {AbstractLoadableActions, StateWithLoadables,} from '../AbstractLoadableActions';
import {TranslationService} from '../../service/TranslationService';
import {AppState} from '../index';
import {useSelector} from 'react-redux';
import {LanguageDTO} from '../../service/response.types';
import {ActionType} from '../Action';
import {LanguageActions} from '../languages/LanguageActions';
import {ProjectPreferencesService} from '../../service/ProjectPreferencesService';

export type TranslationEditingType = {
  key: string;
  languageAbbreviation: string;
  initialValue: string;
  newValue: string;
} | null;
export type SourceEditingType = { initialValue: string; newValue: string };

export class TranslationsState extends StateWithLoadables<TranslationActions> {
  selectedLanguages: string[] = [];
  editing: {
    type: 'key' | 'translation';
    data: TranslationEditingType | SourceEditingType | null;
  } | null = null;
  editingAfterConfirmation: {
    type: 'key' | 'translation';
    data: TranslationEditingType | SourceEditingType | null;
  } | null = null;
}

const service = container.resolve(TranslationService);
const languageActions = container.resolve(LanguageActions);

@singleton()
export class TranslationActions extends AbstractLoadableActions<TranslationsState> {
  constructor(private selectedLanguagesService: ProjectPreferencesService) {
    super(new TranslationsState());
  }

  select = this.createAction('SELECT_LANGUAGES', (langs) => langs).build.on(
    (state, action) =>
      <TranslationsState>{ ...state, selectedLanguages: action.payload }
  );

  otherEditionConfirm = this.createAction(
    'OTHER_EDITION_CONFIRM',
    () => {}
  ).build.on((state, action) => ({
    ...state,
    //@ts-ignore
    editing: { ...state.editingAfterConfirmation },
    editingAfterConfirmation: null,
  }));

  otherEditionCancel = this.createAction(
    'OTHER_EDITION_CANCEL',
    () => {}
  ).build.on((state, action) => ({
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
    (data: TranslationEditingType) => data
  ).build.on((state, action) => {
    const needsConfirmation =
      state.editing &&
      state.editing.data?.initialValue !== state.editing.data?.newValue;
    return {
      ...state,
      [needsConfirmation ? 'editingAfterConfirmation' : 'editing']: {
        type: 'translation',
        data: { ...action.payload },
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

  readonly loadableDefinitions = {
    translations: this.createLoadableDefinition(
      service.getTranslations,
      (state, action) => {
        return { ...state, selectedLanguages: action.payload.params.languages };
      }
    ),
    createKey: this.createLoadableDefinition(service.createKey),
    editKey: this.createLoadableDefinition(service.editKey, (state, action) => {
      return { ...state, editingAfterConfirmation: null, editing: null };
    }),
    setTranslations: this.createLoadableDefinition(
      service.setTranslations,
      (state, action) => {
        return { ...state, editingAfterConfirmation: null, editing: null };
      }
    ),
    delete: this.createLoadableDefinition(service.deleteKey),
  };

  useSelector<T>(selector: (state: TranslationsState) => T): T {
    return useSelector((state: AppState) => selector(state.translations));
  }

  customReducer(
    state: TranslationsState,
    action: ActionType<any>,
    appState
  ): TranslationsState {
    appState = appState as AppState; // otherwise circular reference
    switch (action.type) {
      case languageActions.loadableActions.list.fulfilledType:
        //reseting translations state on language change
        return {
          ...state,
          selectedLanguages: Array.from(
            this.selectedLanguagesService.getUpdated(
              appState.projects.loadables.project.data.id,
              new Set(action.payload.map((l: LanguageDTO) => l.abbreviation))
            )
          ),
          loadables: {
            ...state.loadables,
            translations: this.initialState.loadables.translations,
          },
        };
    }
    return state;
  }

  get prefix(): string {
    return 'TRANSLATIONS';
  }
}
