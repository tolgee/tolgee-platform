import { useState } from 'react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useLatchedSearchVisibility } from 'tg.views/projects/useLatchedSearchVisibility';

export const usePublicProjectsList = () => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const loadable = useApiQuery({
    url: '/v2/public/projects/with-stats',
    method: 'get',
    query: {
      page,
      size: 20,
      search,
      sort: ['name,asc'],
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

  return { loadable, showSearch, search, onSearch, onPageChange: setPage };
};
