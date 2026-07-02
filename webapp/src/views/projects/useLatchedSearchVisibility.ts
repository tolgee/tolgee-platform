import { useEffect, useState } from 'react';

const MAX_PROJECTS_WITHOUT_SEARCH = 5;

/**
 * Decides whether a project list should show its search field. Latches to true once relevant:
 * with keepPreviousData, clearing a search holds the filtered (small) totalElements while the
 * refetch is in flight, which would otherwise hide the field mid-interaction and drop focus.
 */
export const useLatchedSearchVisibility = (
  totalElements: number | undefined,
  search: string
) => {
  const relevant =
    Boolean(search) || (totalElements ?? 0) > MAX_PROJECTS_WITHOUT_SEARCH;
  const [latched, setLatched] = useState(false);
  useEffect(() => {
    if (relevant) {
      setLatched(true);
    }
  }, [relevant]);
  // `relevant` turns the field on synchronously (no first-paint flash); `latched` keeps it on
  // when `relevant` transiently goes false during a keepPreviousData refetch.
  return relevant || latched;
};
