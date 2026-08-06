import { T } from '@tolgee/react';
import { Box, Typography } from '@mui/material';

import { useDateFormatter } from 'tg.hooks/useLocale';

type Props = {
  createdAt?: number;
  periodStart?: number;
  periodEnd?: number;
};

export const ActivePlanDates = ({
  createdAt,
  periodStart,
  periodEnd,
}: Props) => {
  const formatDate = useDateFormatter();

  return (
    <Box display="grid">
      {createdAt && (
        <Typography
          variant="caption"
          data-cy="billing-active-card-subscribed-at"
        >
          <T keyName="active-plan-subscribed-at-tooltip" />:{' '}
          {formatDate(createdAt)}
        </Typography>
      )}
      {Boolean(periodStart && periodEnd) && (
        <Typography
          variant="caption"
          data-cy="billing-active-card-current-period"
        >
          <T
            keyName="active-plan-current-period"
            params={{ start: periodStart, end: periodEnd }}
          />
        </Typography>
      )}
    </Box>
  );
};
