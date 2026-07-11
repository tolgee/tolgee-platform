import { useMemo } from 'react';

import {
  useApiInfiniteQuery,
  useApiMutation,
} from 'tg.service/http/useQueryApi';
import { TmRow } from 'tg.ee.module/translationMemory/components/content/TranslationMemoryEntryRow';

const PAGE_SIZE = 50;

type Args = {
  organizationId: number;
  translationMemoryId: number;
  search?: string;
  targetLanguageTag?: string;
};

/**
 * Wraps the infinite-paginated `entries` query plus the `entryIds` mutation that powers
 * "Select all". Returns the same surface the list view used inline before the split — row
 * data, pagination helpers, total count, and an async resolver for every stored entry id
 * matching the current filter.
 */
export function useTmEntriesData({
  organizationId,
  translationMemoryId,
  search,
  targetLanguageTag,
}: Args) {
  const entriesPath = { organizationId, translationMemoryId };
  const entriesQuery = {
    size: PAGE_SIZE,
    search: search || undefined,
    targetLanguageTag,
  };
  const entries = useApiInfiniteQuery({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries',
    method: 'get',
    path: entriesPath,
    query: entriesQuery,
    options: {
      keepPreviousData: true,
      noGlobalLoading: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: entriesPath,
            query: { ...entriesQuery, page: lastPage.page!.number! + 1 },
          };
        }
        return null;
      },
    },
  });

  // Backend returns one model per row already (one STORED bucket = one row; one VIRTUAL
  // origin = one row). Flatten every loaded page into a single array.
  const rows = useMemo<TmRow[]>(
    () =>
      (entries.data?.pages ?? []).flatMap(
        (p) => p._embedded?.translationMemoryRows ?? []
      ),
    [entries.data]
  );

  const totalElements = entries.data?.pages?.[0]?.page?.totalElements ?? 0;

  // Each selectable row is identified by the representative entry id (the first cell that
  // has one). Read-only (virtual) rows carry no entryId — the backend excludes them.
  const getAllStoredEntryIdsMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries/entryIds',
    method: 'get',
  });

  const getAllGroupIds = async (): Promise<number[]> => {
    if (totalElements === 0) return [];
    const data = await getAllStoredEntryIdsMutation.mutateAsync({
      path: { organizationId, translationMemoryId },
      query: { search },
    });
    return data._embedded?.longList ?? [];
  };

  return { entries, rows, totalElements, getAllGroupIds };
}
