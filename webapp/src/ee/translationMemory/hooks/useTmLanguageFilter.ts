import { useCallback, useMemo, useState } from 'react';
import { useDebounce } from 'use-debounce';

import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { tmPreferencesService } from 'tg.ee.module/translationMemory/services/TmPreferencesService';

type OrganizationLanguageModel =
  components['schemas']['OrganizationLanguageModel'];

const LANGUAGE_SEARCH_DEBOUNCE_MS = 500;

type Args = {
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
};

/**
 * State + queries for the per-TM language filter.
 *
 * The selection is persisted per-TM and seeded once: on the first visit (nothing stored),
 * `seedAutoDefault` sets it from the first page's entry languages and persists it.
 */
export function useTmLanguageFilter({
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
}: Args) {
  const [selectedLanguages, setSelectedLanguages] = useState<
    string[] | undefined
  >(() => tmPreferencesService.getForTm(translationMemoryId));

  // True only between the one-time auto-seed and the first explicit change. Keeps the
  // backend filter off so seeding doesn't re-fetch the entries.
  const [isAutoDefault, setIsAutoDefault] = useState(false);

  const [langSearch, setLangSearch] = useState('');
  const [langSearchDebounced] = useDebounce(
    langSearch,
    LANGUAGE_SEARCH_DEBOUNCE_MS
  );

  const langQuery = { search: langSearchDebounced, size: 30 };
  const languagesLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{organizationId}/languages',
    method: 'get',
    path: { organizationId },
    query: langQuery,
    options: {
      keepPreviousData: true,
      refetchOnMount: true,
      noGlobalLoading: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { id: organizationId },
            query: { ...langQuery, page: lastPage.page!.number! + 1 },
          };
        }
        return null;
      },
    },
  });

  const languages = languagesLoadable.data?.pages.flatMap(
    (p) => p._embedded?.languages ?? []
  );

  const fetchMoreLanguages = () => {
    if (languagesLoadable.hasNextPage && !languagesLoadable.isFetching) {
      languagesLoadable.fetchNextPage();
    }
  };

  const seedAutoDefault = useCallback(
    (entryLanguageTags: string[]) => {
      const langs = entryLanguageTags.filter((t) => t !== sourceLanguageTag);
      setSelectedLanguages(langs);
      setIsAutoDefault(true);
      tmPreferencesService.setForTm(translationMemoryId, langs);
    },
    [sourceLanguageTag, translationMemoryId]
  );

  const updateSelectedLanguages = (langs: string[]) => {
    setSelectedLanguages(langs);
    setIsAutoDefault(false);
    tmPreferencesService.setForTm(translationMemoryId, langs);
  };

  const toggleLanguage = (item: OrganizationLanguageModel) => {
    if (item.tag === sourceLanguageTag) return;
    const current = selectedLanguages ?? [];
    const next = current.includes(item.tag)
      ? current.filter((t) => t !== item.tag)
      : [...current, item.tag];
    updateSelectedLanguages(next);
  };

  // Every loaded org language except the base — offered to the create-entry dialog and the
  // empty-state wizard as the full set of pickable target languages.
  const allNonBaseLanguageTags = useMemo(
    () =>
      (languages ?? [])
        .map((l) => l.tag)
        .filter((t) => t !== sourceLanguageTag),
    [languages, sourceLanguageTag]
  );

  // The displayed / checked set: the stored selection, base language excluded.
  const displayLanguages = useMemo(
    () => (selectedLanguages ?? []).filter((t) => t !== sourceLanguageTag),
    [selectedLanguages, sourceLanguageTag]
  );

  // The targetLanguageTag query param: undefined while the selection is the freshly-seeded
  // auto-default (filtering by every first-page language == no filter) or empty; a
  // comma-separated list once the selection is an explicit/stored choice.
  const targetLanguageTag = useMemo(() => {
    if (isAutoDefault) return undefined;
    if (!selectedLanguages || selectedLanguages.length === 0) return undefined;
    return selectedLanguages.join(',');
  }, [isAutoDefault, selectedLanguages]);

  // First two of the selection, used as the Create-entry dialog's default.
  const createDialogInitialTags = useMemo(
    () => displayLanguages.slice(0, 2),
    [displayLanguages]
  );

  return {
    languages,
    languagesLoadable,
    selectedLanguages,
    langSearch,
    setLangSearch,
    fetchMoreLanguages,
    toggleLanguage,
    updateSelectedLanguages,
    seedAutoDefault,
    allNonBaseLanguageTags,
    displayLanguages,
    targetLanguageTag,
    createDialogInitialTags,
  };
}
