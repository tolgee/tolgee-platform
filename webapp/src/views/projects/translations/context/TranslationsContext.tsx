import React, { useState, useRef } from 'react';
import { useQueryClient } from 'react-query';
import { T } from '@tolgee/react';
import ReactList from 'react-list';
import { createProvider } from 'tg.fixtures/createProvider';

import { components, operations } from 'tg.service/apiSchema.generated';
import { invalidateUrlPrefix, useApiQuery } from 'tg.service/http/useQueryApi';
import { container } from 'tsyringe';
import { MessageService } from 'tg.service/MessageService';
import { ProjectPreferencesService } from 'tg.service/ProjectPreferencesService';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { confirmation } from 'tg.hooks/confirmation';
import { useTranslationsInfinite } from './useTranslationsInfinite';
import { useEdit, EditType } from './useEdit';
import { StateType } from 'tg.constants/translationStates';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import {
  usePostKey,
  useDeleteKeys,
  usePutTag,
  useDeleteTag,
  usePutTranslationState,
} from 'tg.service/TranslationHooks';

export type AfterCommand = 'EDIT_NEXT';

type LanguagesType = components['schemas']['LanguageModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];
type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];
type TranslationsQueryType =
  operations['getTranslations']['parameters']['query'];

type ActionType =
  | { type: 'SET_SEARCH'; payload: string }
  | { type: 'SET_SEARCH_IMMEDIATE'; payload: string }
  | { type: 'SET_FILTERS'; payload: FiltersType }
  | { type: 'SET_EDIT'; payload: EditType | undefined }
  | { type: 'SET_EDIT_FORCE'; payload: EditType | undefined }
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
  | { type: 'ADD_TAG'; payload: AddTagPayload; onSuccess?: () => void }
  | { type: 'REMOVE_TAG'; payload: RemoveTagPayload }
  | { type: 'UPDATE_TRANSLATION'; payload: UpdateTranslationPayolad }
  | { type: 'INSERT_TRANSLATION'; payload: AddTranslationPayload }
  | { type: 'REGISTER_ELEMENT'; payload: KeyElement }
  | { type: 'UNREGISTER_ELEMENT'; payload: KeyElement }
  | { type: 'REGISTER_LIST'; payload: ReactList }
  | { type: 'UNREGISTER_LIST'; payload: ReactList };

type CellPosition = {
  keyId: number;
  language: string | undefined;
};

type KeyElement = CellPosition & {
  ref: HTMLElement;
};

export type ViewType = 'LIST' | 'TABLE';

type AddTranslationPayload = KeyWithTranslationsModelType;

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

type ChangeValueType = {
  after?: AfterCommand;
  onSuccess?: () => void;
};

export type FiltersType = Pick<
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
  dataReady: boolean;
  translations?: KeyWithTranslationsModelType[];
  translationsLanguages?: string[];
  translationsTotal?: number;
  languages?: LanguagesType[];
  isLoading?: boolean;
  isFetching?: boolean;
  isFetchingMore?: boolean;
  isEditLoading?: boolean;
  hasMoreToFetch?: boolean;
  search?: string;
  urlSearch: string | undefined;
  selection: number[];
  cursor?: EditType;
  selectedLanguages?: string[];
  view: ViewType;
  filters: FiltersType;
  elementsRef: React.RefObject<Map<string, HTMLElement>>;
  reactList: ReactList | undefined;
};

const messaging = container.resolve(MessageService);
const projectPreferences = container.resolve(ProjectPreferencesService);

export const [
  TranslationsContextProvider,
  useTranslationsDispatch,
  useTranslationsSelector,
] = createProvider(
  (props: {
    projectId: number;
    keyName?: string;
    languages?: string[];
    updateLocalStorageLanguages?: boolean;
    pageSize?: number;
  }) => {
    const queryClient = useQueryClient();
    const [selection, setSelection] = useState<number[]>([]);
    const [view, setView] = useUrlSearchState('view', { defaultVal: 'LIST' });
    const [initialLangs, setInitialLangs] = useState<
      string[] | null | undefined
    >(null);
    const elementsRef = useRef<Map<string, HTMLElement>>(new Map());
    const [reactList, setReactList] = useState<ReactList>();

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
      keyName: props.keyName,
      pageSize: props.pageSize,
      updateLocalStorageLanguages: props.updateLocalStorageLanguages,
      // when initial langs are null, fetching is postponed
      initialLangs: props.languages || initialLangs,
    });

    const edit = useEdit({
      projectId: props.projectId,
      translations: translations.fixedTranslations,
    });

    const handleTranslationsReset = () => {
      edit.setPosition(undefined);
      setSelection([]);
    };

    const focusCell = (cell: CellPosition) => {
      const element = elementsRef.current?.get(
        JSON.stringify({ keyId: cell.keyId, language: cell.language })
      );
      element?.focus();
    };

    const setFocusPosition = (pos: EditType | undefined) => {
      // make it async if someone is stealing focus
      setTimeout(() => {
        // focus cell when closing editor
        if (pos === undefined && edit.position) {
          const position = {
            keyId: edit.position.keyId,
            language: edit.position.language,
          };
          focusCell(position);
        }
        edit.setPosition(pos);
      });
    };

    const postKey = usePostKey();
    const deleteKeys = useDeleteKeys();
    const putTag = usePutTag();
    const deleteTag = useDeleteTag();
    const putTranslationState = usePutTranslationState();

    const dispatch = async (action: ActionType) => {
      switch (action.type) {
        case 'SET_SEARCH':
          translations.setSearch(action.payload);
          handleTranslationsReset();
          return;
        case 'SET_SEARCH_IMMEDIATE':
          translations.setUrlSearch(action.payload);
          handleTranslationsReset();
          return;
        case 'SET_FILTERS':
          translations.setFilters(action.payload);
          handleTranslationsReset();
          return;
        case 'SET_EDIT':
          if (edit.position?.changed) {
            setFocusPosition({ ...edit.position, mode: 'editor' });
            confirmation({
              title: <T>translations_unsaved_changes_confirmation_title</T>,
              message: <T>translations_unsaved_changes_confirmation</T>,
              cancelButtonText: <T>back_to_editing</T>,
              confirmButtonText: <T>translations_cell_save</T>,
              onConfirm: () => {
                dispatch({
                  type: 'CHANGE_FIELD',
                  payload: {
                    onSuccess() {
                      setFocusPosition(action.payload);
                    },
                  },
                });
              },
            });
          } else {
            setFocusPosition(action.payload);
          }
          return;
        case 'SET_EDIT_FORCE':
          setFocusPosition(action.payload);
          return;
        case 'UPDATE_EDIT':
          edit.setPosition((pos) =>
            pos ? { ...pos, ...action.payload } : pos
          );
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
          if (!edit.position) {
            return;
          }
          const { keyId, language, value } = edit.position;
          if (!language && !value) {
            // key can't be empty
            messaging.error(<T>global_empty_value</T>);
            return;
          }
          try {
            if (language) {
              // update translation
              await edit
                .mutateTranslation({
                  ...action.payload,
                  value: value as string,
                  keyId,
                  language,
                })
                .then((data) => {
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
                .mutateTranslationKey({
                  ...action.payload,
                  value: value as string,
                  keyId,
                  language,
                })
                .then(() =>
                  translations.updateTranslationKey(keyId, { keyName: value })
                );
            }
            doAfterCommand(action.payload.after);
            action.payload.onSuccess?.();
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
          putTranslationState.mutate(
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
        case 'ADD_TAG':
          return putTag
            .mutateAsync({
              path: { projectId: props.projectId, keyId: action.payload.keyId },
              content: { 'application/json': { name: action.payload.name } },
            })
            .then((data) => {
              const previousTags =
                translations.fixedTranslations
                  ?.find((key) => key.keyId === action.payload.keyId)
                  ?.keyTags.filter((t) => t.id !== data.id) || [];
              translations.updateTranslationKey(action.payload.keyId, {
                keyTags: [...previousTags, data!],
              });
              invalidateUrlPrefix(queryClient, '/v2/projects/{projectId}/tags');
              action.onSuccess?.();
            })
            .catch((e) => {
              const parsed = parseErrorResponse(e);
              parsed.forEach((error) => messaging.error(<T>{error}</T>));
              // return never fullfilling promise to prevent after action
              return new Promise(() => {});
            });
        case 'REMOVE_TAG':
          return deleteTag
            .mutateAsync({
              path: {
                keyId: action.payload.keyId,
                tagId: action.payload.tagId,
                projectId: props.projectId,
              },
            })
            .then(() => {
              const previousTags = translations.fixedTranslations?.find(
                (key) => key.keyId === action.payload.keyId
              )?.keyTags;
              invalidateUrlPrefix(queryClient, '/v2/projects/{projectId}/tags');
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
        case 'INSERT_TRANSLATION':
          translations.insertAsFirst(action.payload);
          return;
        case 'REGISTER_ELEMENT':
          return elementsRef.current.set(
            JSON.stringify({
              keyId: action.payload.keyId,
              language: action.payload.language,
            }),
            action.payload.ref
          );
        case 'UNREGISTER_ELEMENT':
          return elementsRef.current.delete(
            JSON.stringify({
              keyId: action.payload.keyId,
              language: action.payload.language,
            })
          );
        case 'REGISTER_LIST':
          setReactList(action.payload);
          return;
        case 'UNREGISTER_LIST':
          if (reactList === action.payload) {
            setReactList(undefined);
          }
          return;
      }
    };

    const doAfterCommand = (command?: AfterCommand) => {
      switch (command) {
        case 'EDIT_NEXT':
          edit.moveEditToDirection('DOWN');
          return;

        default:
          setFocusPosition(undefined);
      }
    };

    const dataReady = Boolean(languages.data && translations.fixedTranslations);

    const state = {
      dataReady,
      translations: dataReady ? translations.fixedTranslations : undefined,
      translationsLanguages: translations.selectedLanguages,
      translationsTotal:
        translations.totalCount !== undefined
          ? translations.totalCount
          : undefined,
      languages: dataReady ? languages.data?._embedded?.languages : undefined,
      isLoading: translations.isLoading || languages.isLoading,
      isFetching:
        translations.isFetching ||
        languages.isFetching ||
        deleteKeys.isLoading ||
        putTranslationState.isLoading ||
        putTag.isLoading ||
        deleteTag.isLoading ||
        postKey.isLoading,
      isEditLoading: edit.isLoading,
      isFetchingMore: translations.isFetchingNextPage,
      hasMoreToFetch: translations.hasNextPage,
      search: translations.search as string,
      urlSearch: translations.urlSearch,
      filters: translations.filters,
      cursor: edit.position,
      selection,
      selectedLanguages:
        translations.query?.languages || translations.selectedLanguages,
      view: view as ViewType,
      elementsRef,
      reactList,
    };

    return [state, dispatch];
  }
);
