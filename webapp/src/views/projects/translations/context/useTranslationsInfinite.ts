import { useState, useMemo, useEffect } from 'react';
import { container } from 'tsyringe';

import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { components, operations } from 'tg.service/apiSchema.generated';
import { ProjectPreferencesService } from 'tg.service/ProjectPreferencesService';
import { InfiniteData } from 'react-query';
import { useQueryState } from 'tg.hooks/useQueryState';

const projectPreferences = container.resolve(ProjectPreferencesService);

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

type Props = {
  projectId: number;
  initialLangs: string[] | null | undefined;
};

const flattenKeys = (
  data: InfiniteData<TranslationsResponse>
): KeyWithTranslationsModelType[] =>
  data?.pages.filter(Boolean).flatMap((p) => p._embedded?.keys || []) || [];

export const useTranslationsInfinite = (props: Props) => {
  const [filters, setFilters] = useQueryState('filters', JSON.stringify({}));
  const parsedFilters = (filters ? JSON.parse(filters) : {}) as FiltersType;
  // wait for initialLangs to not be null
  const [enabled, setEnabled] = useState(props.initialLangs !== null);

  const [search, setSearch] = useQueryState('search', '');

  const [query, setQuery] = useState<Omit<TranslationsQueryType, 'search'>>({
    size: PAGE_SIZE,
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
    query: { ...query, ...parsedFilters, search },
    options: {
      // fetch after languages are loaded,
      // so we dont't try to fetch nonexistant languages
      enabled,
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
              cursor: lastPage.nextCursor,
            },
          };
        }
      },
      onSuccess(data) {
        const flatKeys = flattenKeys(data);
        projectPreferences.setForProject(
          props.projectId,
          data.pages[0].selectedLanguages.map((l) => l.tag)
        );
        if (data?.pages.length === 1) {
          // reset fixed translations when fetching first page
          // keep unsaved translations
          setFixedTranslations((data) => [
            ...(data || []).filter((key) => key.keyId < 0),
            ...flatKeys,
          ]);
        } else {
          // add only nonexistent keys
          const newKeys =
            flatKeys.filter(
              (k) => !fixedTranslations?.find((ft) => ft.keyId === k.keyId)
            ) || [];
          setFixedTranslations([...(fixedTranslations || []), ...newKeys]);
        }
      },
    },
  });

  const refetchTranslations = () => {
    // force refetch from first page
    translations.remove();
    // remove unsaved translations
    setFixedTranslations((data) => data?.filter((key) => key.keyId >= 0));
  };

  const updateSearch = (value: string) => {
    setSearch(value);
    refetchTranslations();
  };

  const updateQuery = (q: Partial<typeof query>) => {
    const newQuery = { ...query, ...q };
    setQuery({
      ...newQuery,
      languages: newQuery.languages?.length ? newQuery.languages : undefined,
    });
    refetchTranslations();
  };

  const updateFilters = (filters: FiltersType) => {
    setFilters(JSON.stringify(filters));
    refetchTranslations();
  };

  const updateTranslationKey = (
    keyId: number,
    value: Partial<KeyWithTranslationsModelType>
  ) => {
    setFixedTranslations(
      fixedTranslations?.map((k) => {
        if (k.keyId === keyId) {
          return { ...k, ...value };
        } else {
          return k;
        }
      })
    );
  };

  const updateTranslation = (
    keyId: number,
    language: string,
    value: Partial<TranslationModel> | undefined
  ) => {
    setFixedTranslations(
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

  return {
    isLoading: translations.isLoading,
    isFetching: translations.isFetching,
    isFetchingNextPage: translations.isFetchingNextPage,
    hasNextPage: translations.hasNextPage,
    query,
    filters: parsedFilters,
    fetchNextPage: translations.fetchNextPage,
    selectedLanguages: translations.data?.pages[0]?.selectedLanguages.map(
      (l) => l.tag
    ),
    data: fixedTranslations,
    refetchTranslations,
    updateQuery,
    search,
    updateSearch,
    updateFilters,
    updateTranslationKey,
    updateTranslation,
    setTranslations: setFixedTranslations,
  };
};
