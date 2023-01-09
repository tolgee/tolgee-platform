import { useEffect, useMemo, useState } from 'react';
import { InfiniteData } from 'react-query';
import { container } from 'tsyringe';
import { useDebouncedCallback } from 'use-debounce';
import { T } from '@tolgee/react';

import {
  useApiInfiniteQuery,
  useApiMutation,
} from 'tg.service/http/useQueryApi';
import { components, operations } from 'tg.service/apiSchema.generated';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { ProjectPreferencesService } from 'tg.service/ProjectPreferencesService';
import { putBaseLangFirst } from 'tg.fixtures/putBaseLangFirst';
import {
  ChangeScreenshotNum,
  KeyUpdateData,
  UpdateTranslation,
} from '../types';
import { useMessage } from 'tg.hooks/useSuccessMessage';

const MAX_LANGUAGES = 10;
const PAGE_SIZE = 60;

type TranslationsQueryType =
  operations['getTranslations']['parameters']['query'];
export type DeletableKeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'] & { deleted?: boolean };
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
  | 'filterNamespace'
>;

const projectPreferences = container.resolve(ProjectPreferencesService);

type Props = {
  projectId: number;
  keyName?: string;
  keyNamespace?: string;
  keyId?: number;
  initialLangs: string[] | null | undefined;
  pageSize?: number;
  updateLocalStorageLanguages?: boolean;
  baseLang: string | undefined;
};

const flattenKeys = (
  data: InfiniteData<TranslationsResponse>
): DeletableKeyWithTranslationsModelType[] =>
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
  const [_, setUrlLanguages] = useUrlSearchState('languages', {});

  const [urlSearch, _setUrlSearch] = useUrlSearchState('search', {
    defaultVal: '',
  });

  const messaging = useMessage();

  const [search, _setSearch] = useState(urlSearch);
  const [languages, _setLanguages] = useState<string[] | undefined>(undefined);

  const [manuallyInserted, setManuallyInserted] = useState(0);

  const [query, setQuery] = useState<Omit<TranslationsQueryType, 'search'>>({
    size: props.pageSize || PAGE_SIZE,
    sort: ['keyNamespace', 'keyName'],
    languages: undefined,
  });

  useEffect(() => {
    const timer = setTimeout(() => {
      if (query.languages !== languages) {
        updateQuery({ languages });
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [languages]);

  useEffect(() => {
    if (props.initialLangs !== null) {
      const languages = props.initialLangs?.slice(0, MAX_LANGUAGES);
      setEnabled(true);
      setQuery({
        ...query,
        languages: languages,
      });
      _setLanguages(languages);
    }
  }, [props.initialLangs]);

  const [fixedTranslations, setFixedTranslations] = useState<
    DeletableKeyWithTranslationsModelType[] | undefined
  >();

  const path = useMemo(
    () => ({ projectId: props.projectId }),
    [props.projectId]
  );

  const filterNamespace =
    props.keyNamespace !== undefined
      ? [props.keyNamespace]
      : parsedFilters.filterNamespace;

  const requestQuery = {
    ...query,
    ...parsedFilters,
    filterKeyName: props.keyName ? [props.keyName] : undefined,
    filterNamespace,
    filterKeyId: props.keyId ? [props.keyId] : undefined,
    search: urlSearch as string,
  };

  const translations = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/translations',
    method: 'get',
    path,
    query: requestQuery,
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
              ...requestQuery,
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

  const allIds = useApiMutation({
    url: '/v2/projects/{projectId}/translations/select-all',
    method: 'get',
  });

  const getAllIds = () =>
    allIds.mutateAsync({
      path: { projectId: props.projectId },
      query: requestQuery,
    });

  const insertAsFirst = (data: DeletableKeyWithTranslationsModelType) => {
    setFixedTranslations((translations) => [data, ...(translations || [])]);
    setManuallyInserted((num) => num + 1);
  };

  const refetchTranslations = (callback?: () => any) => {
    // force refetch from first page
    translations.remove();
    callback?.();
    window?.scrollTo(0, 0);
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

  const setLanguages = (value: string[] | undefined) => {
    if (value && value.length > 10) {
      messaging.error(
        <T
          keyName="translations_languages_limit_reached"
          params={{ max: MAX_LANGUAGES }}
        />
      );
      return;
    }
    if (props.updateLocalStorageLanguages) {
      projectPreferences.setForProject(props.projectId, value);
    }
    // override url languages
    setUrlLanguages(undefined);
    _setLanguages(value?.length ? value : undefined);
  };

  const updateQuery = (q: Partial<typeof query>) => {
    refetchTranslations(() => {
      setQuery({ ...query, ...q });
    });
  };

  const setFilters = (filters: FiltersType) => {
    refetchTranslations(() => {
      _setFilters(JSON.stringify(filters));
    });
  };

  const updateTranslationKeys = (data: KeyUpdateData[]) => {
    setFixedTranslations((fixedTranslations) => {
      let result = fixedTranslations;
      data.forEach((mod) => {
        result = result?.map((k) => {
          if (k.keyId === mod.keyId) {
            return { ...k, ...mod.value };
          } else {
            return k;
          }
        });
      });
      return result;
    });
  };

  const updateScreenshotCount = (data: ChangeScreenshotNum) =>
    updateTranslationKeys([
      { keyId: data.keyId, value: { screenshotCount: data.screenshotCount } },
    ]);

  const changeTranslations = (
    data: {
      keyId: number;
      language: string;
      value: Partial<TranslationModel> | undefined;
    }[]
  ) => {
    setFixedTranslations((fixedTranslations) => {
      let result = fixedTranslations;
      data.forEach((mod) => {
        result = result?.map((k) => {
          if (k.keyId === mod.keyId) {
            return {
              ...k,
              translations: {
                ...k.translations,
                [mod.language]: mod.value
                  ? {
                      ...k.translations[mod.language],
                      ...mod.value,
                    }
                  : (undefined as any),
              },
            };
          }
          return k;
        });
      });
      return result;
    });
  };

  const updateTranslation = (data: UpdateTranslation) =>
    changeTranslations([
      { keyId: data.keyId, language: data.lang, value: data.data },
    ]);

  const totalCount = translations.data?.pages[0].page?.totalElements;

  const currentFetchedLangs = useMemo(() => {
    const langs = translations.data?.pages[0]?.selectedLanguages.map(
      (l) => l.tag
    );

    if (languages) {
      // sort selected languages
      langs?.sort((l1, l2) => languages!.indexOf(l1) - languages!.indexOf(l2));
    }
    return langs;
  }, [translations.data]);

  // memoize so we keep the same reference when possible
  const [selectedLanguages, translationsLanguages] = useMemo(
    () => [
      putBaseLangFirst(languages || currentFetchedLangs, props.baseLang),
      putBaseLangFirst(currentFetchedLangs, props.baseLang),
    ],
    [languages, currentFetchedLangs, props.baseLang]
  );

  return {
    isLoading: translations.isLoading,
    isFetching: translations.isFetching,
    isFetchingNextPage: translations.isFetchingNextPage,
    isLoadingAllIds: allIds.isLoading,
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
    changeTranslations,
    updateQuery,
    search,
    setSearch,
    setLanguages,
    setUrlSearch,
    setFilters,
    updateTranslationKeys,
    updateTranslation,
    insertAsFirst,
    urlSearch: urlSearch as string | undefined,
    updateScreenshotCount,
    getAllIds,
  };
};
