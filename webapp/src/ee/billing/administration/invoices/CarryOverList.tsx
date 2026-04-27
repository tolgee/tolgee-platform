import { FC, useState } from 'react';
import { Paper } from '@mui/material';

import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { CarryOverRow } from './CarryOverRow';

type CarryOverListProps = {
  url:
    | '/v2/administration/billing/carry-overs'
    | '/v2/administration/billing/carry-overs/history';
  emptyMessage: string;
  showSettledBy?: boolean;
};

export const CarryOverList: FC<CarryOverListProps> = ({
  url,
  emptyMessage,
  showSettledBy,
}) => {
  const [page, setPage] = useState(0);

  const loadable = useBillingApiQuery({
    url,
    method: 'get',
    query: {
      page,
      size: 20,
    },
    options: {
      keepPreviousData: true,
    },
  });

  return (
    <PaginatedHateoasList
      onPageChange={setPage}
      listComponent={Paper}
      listComponentProps={{ variant: 'outlined' as const }}
      emptyPlaceholder={emptyMessage}
      renderItem={(item) => (
        <CarryOverRow
          key={`${item.organizationId}-${item.periodStart}`}
          item={item}
          showSettledBy={showSettledBy}
        />
      )}
      loadable={loadable}
    />
  );
};
