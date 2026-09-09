import { useState } from 'react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useLatchedSearchVisibility } from 'tg.views/projects/useLatchedSearchVisibility';

type Options = {
  defaultMyContributionsOnly?: boolean;
};

export const usePublicProjectsList = ({
  defaultMyContributionsOnly,
}: Options = {}) => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [myContributionsOnly, setMyContributionsOnly] = useState(
    Boolean(defaultMyContributionsOnly)
  );

  const loadable = useApiQuery({
    url: '/v2/public/projects/with-stats',
    method: 'get',
    query: {
      page,
      size: 20,
      search,
      sort: ['name,asc'],
      filterContributed: myContributionsOnly,
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
