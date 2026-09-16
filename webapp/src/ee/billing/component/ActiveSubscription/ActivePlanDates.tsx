import { T } from '@tolgee/react';
import { Box, Typography } from '@mui/material';

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
  return (
    <Box display="grid">
      {Boolean(createdAt) && (
        <Typography
          variant="caption"
          data-cy="billing-active-card-subscribed-at"
        >
          <T
            keyName="active-plan-subscribed-at"
            defaultValue="Subscribed at {date, date}"
            params={{ date: createdAt }}
          />
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
