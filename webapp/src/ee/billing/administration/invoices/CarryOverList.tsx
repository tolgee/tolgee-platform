import { FC } from 'react';
import { Paper, Typography } from '@mui/material';

import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
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
  const loadable = useBillingApiQuery({
    url,
    method: 'get',
  });

  const items = loadable.data?._embedded?.carryOvers ?? [];

  if (items.length === 0 && !loadable.isLoading) {
    return (
      <Typography variant="body2" color="text.secondary">
        {emptyMessage}
      </Typography>
    );
  }

  return (
    <Paper variant="outlined">
      {items.map((item) => (
        <CarryOverRow
          key={`${item.organizationId}-${item.periodStart}`}
          item={item}
          showSettledBy={showSettledBy}
        />
      ))}
    </Paper>
  );
};
