import { useState, useMemo, useEffect } from 'react';
import { InfiniteData } from 'react-query';
import { container } from 'tsyringe';
import { useDebouncedCallback } from 'use-debounce';

import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { components, operations } from 'tg.service/apiSchema.generated';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { ProjectPreferencesService } from 'tg.service/ProjectPreferencesService';
import { putBaseLangFirst } from 'tg.fixtures/putBaseLangFirst';
import { ChangeScreenshotNum, UpdateTranslation } from '../types';

const PAGE_SIZE = 60;

type TranslationsQueryType =
  operations['getTranslations']['parameters']['query'];
type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];
type TranslationsResponse =
  components['schemas']['KeysWithTranslationsPageModel'];
type TranslationModel = components['schemas']['TranslationViewModel'];

type FiltersType = Pick<
  TranslationsQueryType,
  | 'filterHasNoScreenshot'
  | 'filterHasScreenshot'
  | 'filterTranslatedAny'
  | 'filterUntranslatedAny'
  | 'filterTranslatedInLang'
  | 'filterUntranslatedInLang'
>;

const projectPreferences = container.resolve(ProjectPreferencesService);

type Props = {
  projectId: number;
  keyName?: string;
  initialLangs: string[] | null | undefined;
  pageSize?: number;
  updateLocalStorageLanguages?: boolean;
  baseLang: string | undefined;
};

const flattenKeys = (
  data: InfiniteData<TranslationsResponse>
): KeyWithTranslationsModelType[] =>
  data?.pages.filter(Boolean).flatMap((p) => p._embedded?.keys || []) || [];

export const useTranslationsService = (props: Props) => {
  const [filters, _setFilters] = useUrlSearchState('filters', {
    defaultVal: JSON.stringify({}),
  });
  const parsedFilters = useMemo(
    () => (filters ? JSON.parse(filters as string) : {}) as FiltersType,
    [filters]
  );
  // wait for initialLangs to not be null
  const [enabled, setEnabled] = useState(props.initialLangs !== null);

  const [urlSearch, _setUrlSearch] = useUrlSearchState('search', {
    defaultVal: '',
  });

  const [search, _setSearch] = useState(urlSearch);

  const [manuallyInserted, setManuallyInserted] = useState(0);

  const [query, setQuery] = useState<Omit<TranslationsQueryType, 'search'>>({
    size: props.pageSize || PAGE_SIZE,
    sort: ['keyName'],
    languages: undefined,
  });

  useEffect(() => {
    if (props.initialLangs !== null) {
      setEnabled(true);
      setQuery({
        ...query,
        languages: props.initialLangs,
      });
    }
  }, [props.initialLangs]);

  const [fixedTranslations, setFixedTranslations] = useState<
    KeyWithTranslationsModelType[] | undefined
  >();

  const path = useMemo(
    () => ({ projectId: props.projectId }),
    [props.projectId]
  );

  const translations = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/translations',
    method: 'get',
    path,
    query: {
      ...query,
      ...parsedFilters,
      filterKeyName: props.keyName,
      search: urlSearch as string,
    },
    options: {
      cacheTime: 0,
      // fetch after languages are loaded,
      // so we dont't try to fetch nonexistant languages
      enabled: enabled,
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.nextCursor &&
          lastPage._embedded?.keys?.length === PAGE_SIZE
        ) {
          return {
            path,
            query: {
              ...query,
              ...parsedFilters,
              search: urlSearch,
              cursor: lastPage.nextCursor,
            },
          };
        }
      },
      onSuccess(data) {
        const flatKeys = flattenKeys(data);
        if (data?.pages.length === 1) {
          // reset fixed translations when fetching first page
          setFixedTranslations(flatKeys);
          setManuallyInserted(0);
        } else {
          setFixedTranslations((fixedTranslations) => {
            // add only nonexistent keys
            const newKeys =
              flatKeys.filter(
                (k) => !fixedTranslations?.find((ft) => ft.keyId === k.keyId)
              ) || [];
            return [...(fixedTranslations || []), ...newKeys];
          });
        }
      },
    },
  });

  const insertAsFirst = (data: KeyWithTranslationsModelType) => {
    setFixedTranslations((translations) => [data, ...(translations || [])]);
    setManuallyInserted((num) => num + 1);
  };

  const refetchTranslations = (callback?: () => any) => {
    // force refetch from first page
    translations.remove();
    callback?.();
    setTimeout(() => {
      // make sure that we are refetching, but prevent double fetch
      translations.refetch();
    });
  };

  const setUrlSearch = (value: string) => {
    refetchTranslations(() => {
      _setUrlSearch(value);
      _setSearch(value);
    });
  };

  const setUrlSearchDelayed = useDebouncedCallback((value: string) => {
    refetchTranslations(() => {
      _setUrlSearch(value);
    });
  }, 500);

  const setSearch = (value: string) => {
    _setSearch(value);
    setUrlSearchDelayed(value);
  };

  const updateQuery = (q: Partial<typeof query>) => {
    const newQuery = { ...query, ...q };
    const queryWithLanguages = {
      ...newQuery,
      languages: newQuery.languages?.length ? newQuery.languages : undefined,
    };
    if (props.updateLocalStorageLanguages) {
      projectPreferences.setForProject(
        props.projectId,
        queryWithLanguages.languages
      );
    }
    refetchTranslations(() => {
      setQuery(queryWithLanguages);
    });
  };

  const setFilters = (filters: FiltersType) => {
    refetchTranslations(() => {
      _setFilters(JSON.stringify(filters));
    });
  };

  const updateTranslationKey = (
    keyId: number,
    value: Partial<KeyWithTranslationsModelType>
  ) => {
    setFixedTranslations((fixedTranslations) =>
      fixedTranslations?.map((k) => {
        if (k.keyId === keyId) {
          return { ...k, ...value };
        } else {
          return k;
        }
      })
    );
  };

  const updateScreenshotCount = (data: ChangeScreenshotNum) =>
    updateTranslationKey(data.keyId, { screenshotCount: data.screenshotCount });

  const changeTranslation = (
    keyId: number,
    language: string,
    value: Partial<TranslationModel> | undefined
  ) => {
    setFixedTranslations((fixedTranslations) =>
      fixedTranslations?.map((k) => {
        if (k.keyId === keyId) {
          return {
            ...k,
            translations: {
              ...k.translations,
              [language]: value
                ? {
                    ...k.translations[language],
                    ...value,
                  }
                : (undefined as any),
            },
          };
        } else {
          return k;
        }
      })
    );
  };

  const updateTranslation = (data: UpdateTranslation) =>
    changeTranslation(data.keyId, data.lang, data.data);

  const totalCount = translations.data?.pages[0].page?.totalElements;

  const selectedLangs = useMemo(() => {
    const langs = translations.data?.pages[0]?.selectedLanguages.map(
      (l) => l.tag
    );

    if (query.languages) {
      // sort selected languages
      langs?.sort(
        (l1, l2) => query.languages!.indexOf(l1) - query.languages!.indexOf(l2)
      );
    }

    return langs;
  }, [translations.data]);

  // memoize so we keep the same reference when possible
  const [selectedLanguages, translationsLanguages] = useMemo(
    () => [
      putBaseLangFirst(query?.languages || selectedLangs, props.baseLang),
      putBaseLangFirst(selectedLangs, props.baseLang),
    ],
    [query?.languages, selectedLangs, props.baseLang]
  );

  return {
    isLoading: translations.isLoading,
    isFetching: translations.isFetching,
    isFetchingNextPage: translations.isFetchingNextPage,
    hasNextPage: translations.hasNextPage,
    query,
    filters: parsedFilters,
    fetchNextPage: translations.fetchNextPage,
    selectedLanguages,
    translationsLanguages,
    data: translations.data,
    fixedTranslations,
    totalCount:
      totalCount !== undefined ? totalCount + manuallyInserted : undefined,
    refetchTranslations,
    changeTranslation,
    updateQuery,
    search,
    setSearch,
    setUrlSearch,
    setFilters,
    updateTranslationKey,
    updateTranslation,
    insertAsFirst,
    urlSearch: urlSearch as string | undefined,
    updateScreenshotCount,
  };
};
