import React, { useState, useRef, useCallback } from 'react';
import { createContext, useContext } from 'use-context-selector';
import { T } from '@tolgee/react';

import { components, operations } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { container } from 'tsyringe';
import { MessageService } from 'tg.service/MessageService';
import { ProjectPreferencesService } from 'tg.service/ProjectPreferencesService';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { confirmation } from 'tg.hooks/confirmation';
import { useTranslationsInfinite } from './useTranslationsInfinite';
import { useEdit, Direction, EditType, ChangeValueType } from './useEdit';
import { StateType } from 'tg.constants/translationStates';

type LanguagesType = components['schemas']['LanguageModel'];
type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];
type TranslationsQueryType =
  operations['getTranslations']['parameters']['query'];

type ActionType =
  | { type: 'SET_SEARCH'; payload: string }
  | { type: 'SET_FILTERS'; payload: FiltersType }
  | { type: 'SET_EDIT'; payload: EditType | undefined }
  | { type: 'UPDATE_EDIT'; payload: Partial<EditType> }
  | { type: 'EDIT_NEXT' }
  | { type: 'EDIT_MOVE'; payload: Direction }
  | { type: 'TOGGLE_SELECT'; payload: number }
  | { type: 'CHANGE_FIELD'; payload: ChangeValueType }
  | { type: 'FETCH_MORE' }
  | { type: 'SELECT_LANGUAGES'; payload: string[] | undefined }
  | { type: 'UPDATE_SCREENSHOT_COUNT'; payload: ChangeScreenshotNum }
  | { type: 'CHANGE_VIEW'; payload: ViewType }
  | { type: 'UPDATE_LANGUAGES' }
  | { type: 'DELETE_TRANSLATIONS'; payload: number[] }
  | { type: 'SET_TRANSLATION_STATE'; payload: SetTranslationStatePayload };

export type ViewType = 'TABLE' | 'LIST';

type FiltersType = Pick<
  TranslationsQueryType,
  | 'filterHasNoScreenshot'
  | 'filterHasScreenshot'
  | 'filterTranslatedAny'
  | 'filterUntranslatedAny'
  | 'filterTranslatedInLang'
  | 'filterUntranslatedInLang'
>;

type SetTranslationStatePayload = {
  keyId: number;
  translationId: number;
  language: string;
  state: StateType;
};

type ChangeScreenshotNum = {
  keyId: number;
  screenshotCount: number | undefined;
};

export type TranslationsContextType = {
  translations?: KeyWithTranslationsModelType[];
  languages?: LanguagesType[];
  isLoading?: boolean;
  isFetching?: boolean;
  isFetchingMore?: boolean;
  hasMoreToFetch?: boolean;
  search?: string;
  selection: number[];
  edit?: EditType;
  selectedLanguages?: string[];
  view: ViewType;
  filters: FiltersType;
};

// @ts-ignore
export const TranslationsContext = createContext<TranslationsContextType>(null);
export const DispatchContext =
  // @ts-ignore
  createContext<(action: ActionType) => void>(null);

export const useTranslationsDispatch = () => useContext(DispatchContext);

const messaging = container.resolve(MessageService);
const projectPreferences = container.resolve(ProjectPreferencesService);

export const TranslationsContextProvider: React.FC<{
  projectId: number;
}> = (props) => {
  const dispatchRef = useRef(null as any as (action: ActionType) => void);
  const [selection, setSelection] = useState<number[]>([]);
  const [view, setView] = useState('LIST' as ViewType);
  const [initialLangs, setInitialLangs] = useState<string[] | null | undefined>(
    null
  );

  const languages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: props.projectId },
    query: { size: 1000, sort: ['tag'] },
    options: {
      onSuccess(data) {
        const languages = projectPreferences
          .getForProject(props.projectId)
          ?.filter((l) =>
            data._embedded?.languages?.find((lang) => lang.tag === l)
          );
        // manually set initial langs
        setInitialLangs(languages.length ? languages : undefined);
      },
      cacheTime: 0,
    },
  });

  const translations = useTranslationsInfinite({
    projectId: props.projectId,
    // when initial langs are null, fetching is postponed
    initialLangs,
  });

  const edit = useEdit({
    projectId: props.projectId,
    translations: translations.data,
  });

  const handleTranslationsReset = () => {
    edit.setPosition(undefined);
    setSelection([]);
  };

  const deleteKeys = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{ids}',
    method: 'delete',
  });

  const updateTranslationState = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/set-state/{state}',
    method: 'put',
  });

  dispatchRef.current = (action: ActionType) => {
    switch (action.type) {
      case 'SET_SEARCH':
        translations.updateQuery({ search: action.payload });
        handleTranslationsReset();
        return;
      case 'SET_FILTERS':
        translations.updateFilters(action.payload);
        handleTranslationsReset();
        return;
      case 'SET_EDIT':
        if (edit.position?.changed) {
          confirmation({
            title: <T>translations_leave_save_confirmation</T>,
            message: <T>translations_leave_save_confirmation_message_1</T>,
            cancelButtonText: <T>back_to_editing</T>,
            confirmButtonText: <T>discard_changes</T>,
            onConfirm: () => edit.setPosition(action.payload),
          });
        } else {
          edit.setPosition(action.payload);
        }
        return;
      case 'UPDATE_EDIT':
        edit.setPosition((pos) => (pos ? { ...pos, ...action.payload } : pos));
        return;
      case 'TOGGLE_SELECT': {
        const newSelection = selection.includes(action.payload)
          ? selection.filter((s) => s !== action.payload)
          : [...selection, action.payload];
        setSelection(newSelection);
        return;
      }
      case 'FETCH_MORE':
        translations.fetchNextPage();
        return;
      case 'CHANGE_FIELD': {
        const { keyId, language, value } = action.payload;
        (language
          ? edit.mutateTranslation(action.payload).then((data) => {
              if (data) {
                translations.updateTranslation(
                  keyId,
                  language,
                  data?.translations[language]
                );
              }
            })
          : edit.mutateTranslationKey(action.payload).then(() => {
              translations.updateTranslationKey(keyId, { keyName: value });
            })
        ).catch((e) => {
          const parsed = parseErrorResponse(e);
          parsed.forEach((error) => messaging.error(<T>{error}</T>));
        });
        return;
      }
      case 'SELECT_LANGUAGES':
        translations.updateQuery({ languages: action.payload });
        handleTranslationsReset();
        return;
      case 'UPDATE_LANGUAGES':
        translations.updateQuery({});
        handleTranslationsReset();
        return;
      case 'UPDATE_SCREENSHOT_COUNT':
        translations.updateTranslationKey(action.payload.keyId, {
          screenshotCount: action.payload.screenshotCount,
        });
        return;
      case 'CHANGE_VIEW':
        setView(action.payload);
        return;
      case 'DELETE_TRANSLATIONS':
        confirmation({
          title: <T>translations_delete_selected</T>,
          message: (
            <T parameters={{ count: String(action.payload.length) }}>
              translations_key_delete_confirmation_text
            </T>
          ),
          onConfirm() {
            deleteKeys.mutate(
              {
                path: { projectId: props.projectId, ids: action.payload },
              },
              {
                onSuccess() {
                  translations.refetchTranslations();
                  handleTranslationsReset();
                  messaging.success(
                    <T>Translation grid - Successfully deleted!</T>
                  );
                },
              }
            );
          },
        });
        return;
      case 'SET_TRANSLATION_STATE':
        updateTranslationState.mutate(
          {
            path: {
              projectId: props.projectId,
              translationId: action.payload.translationId,
              state: action.payload.state,
            },
          },
          {
            onSuccess(data) {
              translations.updateTranslation(
                action.payload.keyId,
                action.payload.language,
                data
              );
            },
          }
        );
    }
  };

  // stable dispatch function
  const dispatch = useCallback(
    (action: ActionType) => dispatchRef.current(action),
    [dispatchRef]
  );

  const dataReady = translations.data && languages.data;

  return (
    <DispatchContext.Provider value={dispatch}>
      <TranslationsContext.Provider
        value={{
          translations: dataReady ? translations.data : undefined,
          languages: dataReady
            ? languages.data?._embedded?.languages
            : undefined,
          isLoading: translations.isLoading || languages.isLoading,
          isFetching:
            translations.isFetching ||
            languages.isFetching ||
            edit.isLoading ||
            deleteKeys.isLoading ||
            updateTranslationState.isLoading,
          isFetchingMore: translations.isFetchingNextPage,
          hasMoreToFetch: translations.hasNextPage,
          search: translations.query?.search,
          filters: translations.filters,
          edit: edit.position,
          selection,
          selectedLanguages:
            translations.query?.languages || translations.selectedLanguages,
          view,
        }}
      >
        {props.children}
      </TranslationsContext.Provider>
    </DispatchContext.Provider>
  );
};
