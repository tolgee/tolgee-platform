import { useState } from 'react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useLatchedSearchVisibility } from 'tg.views/projects/useLatchedSearchVisibility';

type Options = {
  // Opt-in: only the community page exposes the "My contributions only" filter. The public homepage
  // view shares this hook and must keep listing every public project.
  contributionFilter?: boolean;
};

export const usePublicProjectsList = ({ contributionFilter }: Options = {}) => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [myContributionsOnly, setMyContributionsOnly] = useState(true);

  const loadable = useApiQuery({
    url: '/v2/public/projects/with-stats',
    method: 'get',
    query: {
      page,
      size: 20,
      search,
      sort: ['name,asc'],
      filterContributed: contributionFilter ? myContributionsOnly : undefined,
    },
    options: {
      keepPreviousData: true,
    },
  });

  const showSearch = useLatchedSearchVisibility(
    loadable.data?.page?.totalElements,
    search
  );

  const onSearch = (value: string) => {
    setSearch(value);
    setPage(0);
  };

  const onToggleMyContributions = (value: boolean) => {
    setMyContributionsOnly(value);
    setPage(0);
  };

  return {
    loadable,
    showSearch,
    search,
    onSearch,
    onPageChange: setPage,
    myContributionsOnly,
    onToggleMyContributions,
  };
};
