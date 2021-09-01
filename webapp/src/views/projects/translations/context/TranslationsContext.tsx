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
import { useEdit, EditType, SetEditType } from './useEdit';
import { StateType } from 'tg.constants/translationStates';

export type AfterCommand = 'EDIT_NEXT' | 'NEW_EMPTY_KEY';

type LanguagesType = components['schemas']['LanguageModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];
type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];
type TranslationsQueryType =
  operations['getTranslations']['parameters']['query'];

type ActionType =
  | { type: 'SET_SEARCH'; payload: string }
  | { type: 'SET_FILTERS'; payload: FiltersType }
  | { type: 'SET_EDIT'; payload: EditType | undefined }
  | { type: 'UPDATE_EDIT'; payload: Partial<EditType> }
  | { type: 'TOGGLE_SELECT'; payload: number }
  | { type: 'CHANGE_FIELD'; payload: ChangeValueType }
  | { type: 'FETCH_MORE' }
  | { type: 'SELECT_LANGUAGES'; payload: string[] | undefined }
  | { type: 'UPDATE_SCREENSHOT_COUNT'; payload: ChangeScreenshotNum }
  | { type: 'CHANGE_VIEW'; payload: ViewType }
  | { type: 'UPDATE_LANGUAGES' }
  | { type: 'DELETE_TRANSLATIONS'; payload: number[] }
  | { type: 'SET_TRANSLATION_STATE'; payload: SetTranslationStatePayload }
  | { type: 'ADD_EMPTY_KEY'; payload?: AddEmptyKeyType }
  | { type: 'ADD_TAG'; payload: AddTagPayload; onSuccess?: () => void }
  | { type: 'REMOVE_TAG'; payload: RemoveTagPayload }
  | { type: 'UPDATE_TRANSLATION'; payload: UpdateTranslationPayolad };

export type ViewType = 'TABLE' | 'LIST';

type UpdateTranslationPayolad = {
  keyId: number;
  lang: string;
  data: Partial<TranslationViewModel>;
};

type RemoveTagPayload = {
  keyId: number;
  tagId: number;
};

type AddTagPayload = {
  keyId: number;
  name: string;
};

type AddEmptyKeyType = {
  prevId?: number;
};

type ChangeValueType = SetEditType & {
  after?: AfterCommand;
};

type FiltersType = Pick<
  TranslationsQueryType,
  | 'filterHasNoScreenshot'
  | 'filterHasScreenshot'
  | 'filterTranslatedAny'
  | 'filterUntranslatedAny'
  | 'filterTranslatedInLang'
  | 'filterUntranslatedInLang'
  | 'filterState'
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

  const createKey = useApiMutation({
    url: '/v2/projects/{projectId}/keys/create',
    method: 'post',
  });

  const addTag = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{keyId}/tags',
    method: 'put',
  });

  const removeTag = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{keyId}/tags/{tagId}',
    method: 'delete',
  });

  dispatchRef.current = async (action: ActionType) => {
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
        if (edit.position && edit.position.keyId < 0) {
          // if selected key was empty - remove it
          translations.setTranslations((translations) =>
            translations?.filter((t) => t.keyId >= 0)
          );
          edit.setPosition(action.payload);
        } else if (edit.position?.changed) {
          edit.setPosition({ ...edit.position, mode: 'editor' });
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
        if (!language && !value) {
          // key can't be empty
          messaging.error(<T>global_empty_value</T>);
          return;
        }
        try {
          if (keyId < 0) {
            // empty key - not created yet
            await createKey
              .mutateAsync({
                path: { projectId: props.projectId },
                content: {
                  'application/json': {
                    name: action.payload.value,
                  },
                },
              })
              .then((data) =>
                translations.updateTranslationKey(keyId, {
                  keyId: data.id,
                  keyName: data.name,
                  keyTags: [],
                  screenshotCount: 0,
                  translations: {},
                })
              )
              .then(() => messaging.success(<T>translations_key_created</T>));
          } else if (language) {
            // update translation
            await edit.mutateTranslation(action.payload).then((data) => {
              if (data) {
                return translations.updateTranslation(
                  keyId,
                  language,
                  data?.translations[language]
                );
              }
            });
          } else {
            // update key
            await edit
              .mutateTranslationKey(action.payload)
              .then(() =>
                translations.updateTranslationKey(keyId, { keyName: value })
              );
          }
          doAfterCommand(action.payload.after);
        } catch (e) {
          const parsed = parseErrorResponse(e);
          parsed.forEach((error) => messaging.error(<T>{error}</T>));
        }
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
                onError(e) {
                  const parsed = parseErrorResponse(e);
                  parsed.forEach((error) => messaging.error(<T>{error}</T>));
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
            onError(e) {
              const parsed = parseErrorResponse(e);
              parsed.forEach((error) => messaging.error(<T>{error}</T>));
            },
          }
        );
        return;
      case 'ADD_EMPTY_KEY': {
        const newId = (action.payload?.prevId || 0) - 1;
        const existingEmptyKey = translations.data?.find(
          (t) => t.keyId === newId
        );
        if (existingEmptyKey) {
          return;
        }
        const newKey = {
          keyId: newId,
          keyName: '',
          keyTags: [],
          screenshotCount: 0,
          translations: {},
        };
        translations.setTranslations((translations) =>
          translations ? [newKey, ...translations] : [newKey]
        );
        edit.setPosition({
          keyId: newKey.keyId,
          keyName: newKey.keyName,
          mode: 'editor',
        });
        return;
      }
      case 'ADD_TAG':
        return addTag
          .mutateAsync({
            path: { projectId: props.projectId, keyId: action.payload.keyId },
            content: { 'application/json': { name: action.payload.name } },
          })
          .then((data) => {
            const previousTags =
              translations.data
                ?.find((key) => key.keyId === action.payload.keyId)
                ?.keyTags.filter((t) => t.id !== data.id) || [];
            translations.updateTranslationKey(action.payload.keyId, {
              keyTags: [...previousTags, data!],
            });
            action.onSuccess?.();
          })
          .catch((e) => {
            const parsed = parseErrorResponse(e);
            parsed.forEach((error) => messaging.error(<T>{error}</T>));
            // return never fullfilling promise to prevent after action
            return new Promise(() => {});
          });
      case 'REMOVE_TAG':
        return removeTag
          .mutateAsync({
            path: {
              keyId: action.payload.keyId,
              tagId: action.payload.tagId,
              projectId: props.projectId,
            },
          })
          .then(() => {
            const previousTags = translations.data?.find(
              (key) => key.keyId === action.payload.keyId
            )?.keyTags;
            translations.updateTranslationKey(action.payload.keyId, {
              keyTags: previousTags?.filter(
                (t) => t.id !== action.payload.tagId
              ),
            });
          })
          .catch((e) => {
            const parsed = parseErrorResponse(e);
            parsed.forEach((error) => messaging.error(<T>{error}</T>));
          });
      case 'UPDATE_TRANSLATION':
        return translations.updateTranslation(
          action.payload.keyId,
          action.payload.lang,
          action.payload.data
        );
    }
  };

  // stable dispatch function
  const dispatch = useCallback(
    (action: ActionType) => dispatchRef.current(action),
    [dispatchRef]
  );

  const doAfterCommand = (command?: AfterCommand) => {
    switch (command) {
      case 'EDIT_NEXT':
        edit.moveEditToDirection('DOWN');
        return;

      case 'NEW_EMPTY_KEY': {
        const prevId = edit.position?.keyId;
        dispatch({ type: 'ADD_EMPTY_KEY', payload: { prevId } });
        return;
      }
      default:
        edit.setPosition(undefined);
    }
  };

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
            updateTranslationState.isLoading ||
            addTag.isLoading ||
            removeTag.isLoading ||
            createKey.isLoading,
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
