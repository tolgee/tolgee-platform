import { Checkbox, FormControlLabel, ListItem } from '@mui/material';
import { useState } from 'react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type Props = {
  ids: number[];
  setIds: (value: number[]) => void;
};

export const ForOrganizationsList = ({ ids, setIds }: Props) => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const listPermitted = useApiQuery({
    url: '/v2/administration/organizations',
    method: 'get',
    query: {
      page,
      size: 10,
      search,
      sort: ['id,desc'],
    },
    options: {
      keepPreviousData: true,
    },
  });

  return (
    <PaginatedHateoasList
      wrapperComponentProps={{ className: 'listWrapper' }}
      onPageChange={setPage}
      onSearchChange={setSearch}
      loadable={listPermitted}
      renderItem={(o) => (
        <ListItem data-cy="administration-organizations-list-item">
          <FormControlLabel
            control={
              <Checkbox
                size="small"
                checked={ids.includes(o.id)}
                onChange={() => {
                  if (ids.includes(o.id)) {
                    setIds(ids.filter((id) => id !== o.id));
                  } else {
                    setIds([...ids, o.id]);
                  }
                }}
              />
            }
            label={o.name}
          />
        </ListItem>
      )}
    />
  );
};
