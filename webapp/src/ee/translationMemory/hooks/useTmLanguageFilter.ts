import { useMemo, useState } from 'react';
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
 * State + queries for the per-TM language filter:
 *   - the paginated org-languages query (with debounced search)
 *   - persisted user selection (per-TM, undefined = "all", [] = explicit "all", [t,…] = subset)
 *   - derived values for downstream consumers (filter param, display order, dialog defaults)
 *
 * Mutators (`toggleLanguage`, `updateSelectedLanguages`) persist through `tmPreferencesService`
 * so selection is sticky across page reloads.
 */
export function useTmLanguageFilter({
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
}: Args) {
  const [selectedLanguages, setSelectedLanguages] = useState<
    string[] | undefined
  >(() => tmPreferencesService.getForTm(translationMemoryId));

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

  const updateSelectedLanguages = (langs: string[]) => {
    setSelectedLanguages(langs);
    tmPreferencesService.setForTm(translationMemoryId, langs);
  };

  const isAllSelected = selectedLanguages === undefined;

  const toggleLanguage = (item: OrganizationLanguageModel) => {
    if (item.tag === sourceLanguageTag) return;
    if (isAllSelected) {
      // Switching from "all" to explicit — deselect this one language
      const allTags = (languages ?? [])
        .map((l) => l.tag)
        .filter((t) => t !== sourceLanguageTag && t !== item.tag);
      updateSelectedLanguages(allTags);
      return;
    }
    if (selectedLanguages!.includes(item.tag)) {
      updateSelectedLanguages(selectedLanguages!.filter((t) => t !== item.tag));
      return;
    }
    updateSelectedLanguages([...selectedLanguages!, item.tag]);
  };

  const allNonBaseLanguageTags = useMemo(
    () =>
      (languages ?? [])
        .map((l) => l.tag)
        .filter((t) => t !== sourceLanguageTag),
    [languages, sourceLanguageTag]
  );

  const displayLanguages = useMemo(() => {
    if (isAllSelected) return allNonBaseLanguageTags;
    return allNonBaseLanguageTags.filter((t) => selectedLanguages!.includes(t));
  }, [allNonBaseLanguageTags, isAllSelected, selectedLanguages]);

  // The targetLanguageTag query param: comma-separated when narrowed, undefined for "all".
  const targetLanguageTag =
    selectedLanguages && selectedLanguages.length > 0
      ? selectedLanguages.join(',')
      : undefined;

  // First two of the user's filter selection, used as the Create-entry dialog's default. Empty
  // when the filter is in "All" mode — the dialog falls back to the first 2 of all langs.
  const createDialogInitialTags = useMemo(() => {
    if (!selectedLanguages || selectedLanguages.length === 0) return [];
    return selectedLanguages.slice(0, 2);
  }, [selectedLanguages]);

  return {
    languages,
    languagesLoadable,
    isAllSelected,
    selectedLanguages,
    langSearch,
    setLangSearch,
    fetchMoreLanguages,
    toggleLanguage,
    updateSelectedLanguages,
    allNonBaseLanguageTags,
    displayLanguages,
    targetLanguageTag,
    createDialogInitialTags,
  };
}
