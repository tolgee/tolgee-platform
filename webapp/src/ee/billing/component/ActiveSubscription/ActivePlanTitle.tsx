import { T } from '@tolgee/react';
import { Box, Typography } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { PlanTitleText } from '../Plan/PlanTitle';

type Status = components['schemas']['SelfHostedEeSubscriptionModel']['status'];

type Props = {
  name: string;
  status: Status;
  createdAt?: number;
  periodStart?: number;
  periodEnd?: number;
  highlightColor: string;
};

export const ActivePlanTitle = ({
  name,
  createdAt,
  periodStart,
  periodEnd,
  highlightColor,
}: Props) => {
  const formatDate = useDateFormatter();

  return (
    <Box sx={{ mb: 1 }}>
      <PlanTitleText sx={{ color: highlightColor, mb: 1 }}>
        {name}
      </PlanTitleText>
      <Box display="grid">
        {createdAt && (
          <Typography variant="caption">
            {createdAt && (
              <Typography variant="caption">
                <T keyName="active-plan-subscribed-at-tooltip" />:{' '}
                {formatDate(createdAt)}
              </Typography>
            )}
          </Typography>
        )}
        {Boolean(periodStart && periodEnd) && (
          <Typography variant="caption">
            <T
              keyName="active-plan-current-period"
              params={{ start: periodStart, end: periodEnd }}
            />
          </Typography>
        )}
      </Box>
    </Box>
  );
};
