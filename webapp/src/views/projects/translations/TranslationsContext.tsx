import React, { useState, useRef, useMemo } from 'react';
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
import {
  flattenKeys,
  updateTranslation,
  updateTranslationKey,
} from './contextTools';
import { useCallback } from 'react';

const PAGE_SIZE = 60;

type LanguagesType = components['schemas']['PagedModelLanguageModel'];
type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];

type ActionType =
  | { type: 'SET_EDIT'; payload: CellLocation | undefined }
  | { type: 'EDIT_NEXT' }
  | { type: 'EDIT_MOVE'; payload: Direction }
  | { type: 'SET_SEARCH'; payload: string }
  | { type: 'TOGGLE_SELECT'; payload: string }
  | { type: 'CHANGE_FIELD'; payload: ChangeValueType }
  | { type: 'FETCH_MORE' };

type Direction = 'UP' | 'DOWN' | 'LEFT' | 'RIGHT';

type ChangeValueType = CellLocation & {
  value: string;
  after?: Direction;
};

type CellLocation = {
  keyId: number;
  keyName: string;
  language?: string;
};

export type TranslationsContextType = {
  translations?: KeyWithTranslationsModelType[];
  languages?: LanguagesType;
  isLoading?: boolean;
  isFetching?: boolean;
  isFetchingMore?: boolean;
  hasMoreToFetch?: boolean;
  search?: string;
  selection: string[];
  edit?: CellLocation;
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
  const [edit, setEdit] = useState<CellLocation | undefined>(undefined);
  const [selection, setSelection] = useState<string[]>([]);
  const [fixedTranslations, setFixedTranslations] = useState(
    [] as KeyWithTranslationsModelType[]
  );
  const path = useMemo(
    () => ({ projectId: props.projectId }),
    [props.projectId]
  );
  const [query, setQuery] = useState({
    search: '',
    size: PAGE_SIZE,
    page: 0,
    sort: ['key'],
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
        if (Number(lastPage.page?.totalPages) > Number(lastPage.page?.number))
          return {
            path,
            query: {
              ...query,
              page: newPage,
            },
          };
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
    setQuery({ ...query, ...q });
    setEdit(undefined);
    // force refetch from first page
    translations.remove();
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
      return edit?.language
        ? key.translations[edit.language]?.text
        : key.keyName;
    }
  };

  const mutateTranslation = async (payload: ChangeValueType) => {
    const { keyId, keyName, language, value } = payload;

    if (payload.value === getEditOldValue()) {
      // value not modified
      return;
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
            updateTranslationKey(fixedTranslations, keyId, value)
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
        setEdit(action.payload);
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
        return mutateTranslation(action.payload)
          .then(() => {
            moveEditToDirection(action.payload.after);
          })
          .catch((e) => {
            const parsed = parseErrorResponse(e);
            parsed.forEach((error) => messaging.error(<T>{error}</T>));
          });
    }
  };

  // stable dispatch function
  const dispatch = useCallback(
    (action: ActionType) => dispatchRef.current(action),
    [dispatchRef]
  );

  return (
    <DispatchContext.Provider value={dispatch}>
      <TranslationsContext.Provider
        value={{
          translations: fixedTranslations,
          languages: languages.data,
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
        }}
      >
        {props.children}
      </TranslationsContext.Provider>
    </DispatchContext.Provider>
  );
};
