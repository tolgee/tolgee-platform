import { useLatchedTrue } from 'tg.hooks/useLatchedTrue';

const MAX_PROJECTS_WITHOUT_SEARCH = 5;

/**
 * Decides whether a project list should show its search field. Latches to true once relevant:
 * with keepPreviousData, clearing a search holds the filtered (small) totalElements while the
 * refetch is in flight, which would otherwise hide the field mid-interaction and drop focus.
 */
export const useLatchedSearchVisibility = (
  totalElements: number | undefined,
  search: string
) =>
  useLatchedTrue(
    Boolean(search) || (totalElements ?? 0) > MAX_PROJECTS_WITHOUT_SEARCH
  );
