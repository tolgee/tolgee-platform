import React, { useState, useRef, useMemo, useCallback } from 'react';
import { createContext, useContext } from 'use-context-selector';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import {
  useApiInfiniteQuery,
  useApiMutation,
  useApiQuery,
} from 'tg.service/http/useQueryApi';
import { container } from 'tsyringe';
import { MessageService } from 'tg.service/MessageService';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { confirmation } from 'tg.hooks/confirmation';
import {
  flattenKeys,
  updateTranslation,
  updateTranslationKey,
} from './contextTools';

const PAGE_SIZE = 60;

type LanguagesType = components['schemas']['LanguageModel'];
type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];

type ActionType =
  | { type: 'SET_EDIT'; payload: EditType | undefined }
  | { type: 'UPDATE_EDIT'; payload: Partial<EditType> }
  | { type: 'EDIT_NEXT' }
  | { type: 'EDIT_MOVE'; payload: Direction }
  | { type: 'SET_SEARCH'; payload: string }
  | { type: 'TOGGLE_SELECT'; payload: number }
  | { type: 'CHANGE_FIELD'; payload: ChangeValueType }
  | { type: 'FETCH_MORE' }
  | { type: 'SELECT_LANGUAGES'; payload: string[] | undefined }
  | { type: 'UPDATE_SCREENSHOT_COUNT'; payload: ChangeScreenshotNum }
  | { type: 'CHANGE_VIEW'; payload: ViewType }
  | { type: 'UPDATE_LANGUAGES' }
  | { type: 'DELETE_TRANSLATIONS'; payload: number[] };

export type ViewType = 'TABLE' | 'LIST';

type Direction = 'UP' | 'DOWN' | 'LEFT' | 'RIGHT';

type ChangeValueType = CellLocation & {
  value: string;
  after?: Direction;
};

type ChangeScreenshotNum = {
  keyId: number;
  screenshotCount: number | undefined;
};

type EditType = CellLocation & {
  savedValue?: string;
  changed?: boolean;
};

type CellLocation = {
  keyId: number;
  keyName: string;
  language?: string;
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
};

// @ts-ignore
export const TranslationsContext = createContext<TranslationsContextType>(null);
export const DispatchContext =
  // @ts-ignore
  createContext<(action: ActionType) => void>(null);

export const useTranslationsDispatch = () => useContext(DispatchContext);

const messaging = container.resolve(MessageService);

export const TranslationsContextProvider: React.FC<{
  projectId: number;
}> = (props) => {
  const dispatchRef = useRef(null as any as (action: ActionType) => void);
  const [edit, setEdit] = useState<EditType | undefined>(undefined);
  const [selection, setSelection] = useState<number[]>([]);
  const [fixedTranslations, setFixedTranslations] = useState(
    [] as KeyWithTranslationsModelType[]
  );
  const [view, setView] = useState('LIST' as ViewType);

  const path = useMemo(
    () => ({ projectId: props.projectId }),
    [props.projectId]
  );
  const [query, setQuery] = useState({
    search: '',
    size: PAGE_SIZE,
    page: 0,
    sort: ['key'],
    languages: undefined as string[] | undefined,
  });

  const translations = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/translations',
    method: 'get',
    path,
    query,
    options: {
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        const newPage = Number(lastPage.page?.number) + 1;
        if (Number(lastPage.page?.totalPages) > newPage) {
          return {
            path,
            query: {
              ...query,
              page: newPage,
            },
          };
        }
      },
      onSuccess(data) {
        const flatKeys = flattenKeys(data);
        if (data?.pages.length === 1) {
          // reset fixed translations when fetching first page
          setFixedTranslations(flatKeys);
        } else {
          // add only nonexistent keys
          const newKeys =
            flatKeys.filter(
              (k) => !fixedTranslations.find((ft) => ft.keyId === k.keyId)
            ) || [];
          setFixedTranslations([...fixedTranslations, ...newKeys]);
        }
      },
    },
  });

  const updateQuery = (q: Partial<typeof query>) => {
    const newQuery = { ...query, ...q };
    setQuery({
      ...newQuery,
      languages: newQuery.languages?.length ? newQuery.languages : undefined,
    });
    setEdit(undefined);
    setSelection([]);
    // force refetch from first page
    translations.remove();
    translations.refetch();
  };

  const languages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: props.projectId },
    query: { size: 1000, sort: ['tag'] },
  });

  const updateValue = useApiMutation({
    url: '/v2/projects/{projectId}/translations',
    method: 'put',
  });

  const updateKey = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{id}',
    method: 'put',
  });

  const deleteKeys = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{ids}',
    method: 'delete',
  });

  const moveEditToDirection = (direction: Direction | undefined) => {
    const currentIndex = fixedTranslations.findIndex(
      (k) => k.keyId === edit?.keyId
    );
    if (currentIndex === -1 || !direction) {
      setEdit(undefined);
      return;
    }
    let nextKey = undefined as KeyWithTranslationsModelType | undefined;
    if (direction === 'DOWN') {
      nextKey = fixedTranslations[currentIndex + 1];
    } else if (direction === 'UP') {
      nextKey = fixedTranslations[currentIndex - 1];
    }
    setEdit(
      nextKey
        ? { ...edit, keyId: nextKey.keyId, keyName: nextKey.keyName }
        : undefined
    );
  };

  const getEditOldValue = (): string | undefined => {
    const key = fixedTranslations.find((k) => k.keyId === edit?.keyId);
    if (key) {
      return (
        (edit?.language
          ? key.translations[edit.language]?.text
          : key.keyName) || ''
      );
    }
  };

  const mutateTranslation = async (payload: ChangeValueType) => {
    const { keyId, keyName, language, value } = payload;

    if (payload.value === getEditOldValue()) {
      // value not modified
      return null;
    }

    if (language) {
      return updateValue
        .mutateAsync({
          path: { projectId: props.projectId },
          content: {
            'application/json': {
              key: keyName,
              translations: {
                [language]: payload.value,
              },
            },
          },
        })
        .then(() =>
          setFixedTranslations(
            updateTranslation(fixedTranslations, keyId, language, value)
          )
        );
    } else {
      return updateKey
        .mutateAsync({
          path: { projectId: props.projectId, id: payload.keyId },
          content: {
            'application/json': {
              name: payload.value,
            },
          },
        })
        .then(() =>
          setFixedTranslations(
            updateTranslationKey(fixedTranslations, keyId, { keyName: value })
          )
        );
    }
  };

  dispatchRef.current = (action: ActionType) => {
    switch (action.type) {
      case 'SET_SEARCH':
        updateQuery({ search: action.payload });
        return;
      case 'SET_EDIT':
        if (edit?.changed) {
          confirmation({
            title: <T>translations_leave_save_confirmation</T>,
            cancelButtonText: <T>back_to_editing</T>,
            confirmButtonText: <T>discard_changes</T>,
            onConfirm: () => setEdit(action.payload),
          });
        } else {
          setEdit(action.payload);
        }

        return;
      case 'UPDATE_EDIT':
        setEdit((edit) => (edit ? { ...edit, ...action.payload } : edit));
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
      case 'CHANGE_FIELD':
        mutateTranslation(action.payload)
          .then(() => {
            moveEditToDirection(action.payload.after);
          })
          .catch((e) => {
            const parsed = parseErrorResponse(e);
            parsed.forEach((error) => messaging.error(<T>{error}</T>));
          });
        return;
      case 'SELECT_LANGUAGES':
        updateQuery({ languages: action.payload });
        return;
      case 'UPDATE_LANGUAGES':
        updateQuery({});
        return;
      case 'UPDATE_SCREENSHOT_COUNT':
        setFixedTranslations(
          updateTranslationKey(fixedTranslations, action.payload.keyId, {
            screenshotCount: action.payload.screenshotCount,
          })
        );
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
                  updateQuery({});
                  messaging.success(
                    <T>Translation grid - Successfully deleted!</T>
                  );
                },
              }
            );
          },
        });
        return;
    }
  };

  // stable dispatch function
  const dispatch = useCallback(
    (action: ActionType) => dispatchRef.current(action),
    [dispatchRef]
  );

  const dataReady = translations.isSuccess && languages.isSuccess;

  return (
    <DispatchContext.Provider value={dispatch}>
      <TranslationsContext.Provider
        value={{
          translations: dataReady ? fixedTranslations : undefined,
          languages: dataReady
            ? languages.data?._embedded?.languages
            : undefined,
          isLoading: translations.isLoading || languages.isLoading,
          isFetching:
            translations.isFetching ||
            languages.isFetching ||
            updateValue.isLoading ||
            updateKey.isLoading,
          isFetchingMore: translations.isFetchingNextPage,
          hasMoreToFetch: translations.hasNextPage,
          search: query.search,
          edit,
          selection,
          selectedLanguages:
            query.languages ||
            translations.data?.pages[0]?.selectedLanguages.map((l) => l.tag),
          view,
        }}
      >
        {props.children}
      </TranslationsContext.Provider>
    </DispatchContext.Provider>
  );
};
