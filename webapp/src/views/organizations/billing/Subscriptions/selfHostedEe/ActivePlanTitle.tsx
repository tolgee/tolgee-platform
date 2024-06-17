import { T } from '@tolgee/react';
import { Box, Tooltip, Typography } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { PlanTitleText } from '../common/PlanTitle';
import { SubscriptionStatus } from '../../common/SubscriptionStatus';

type Status = components['schemas']['SelfHostedEeSubscriptionModel']['status'];

type Props = {
  name: string;
  status: Status;
  createdAt?: number;
  periodStart?: number;
  periodEnd?: number;
};

export const ActivePlanTitle = ({
  name,
  status,
  createdAt,
  periodStart,
  periodEnd,
}: Props) => {
  const formatDate = useDateFormatter();

  const title = Boolean(periodStart && periodEnd) && (
    <span>
      <T
        keyName="active-plan-current-period"
        params={{ start: periodStart, end: periodEnd }}
      />
    </span>
  );

  return (
    <Box sx={{ mb: 2 }}>
      <Box>
        <Box sx={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
          <PlanTitleText>{name}</PlanTitleText>
          <Tooltip title={title}>
            <Box>
              <SubscriptionStatus status={status} />
            </Box>
          </Tooltip>
        </Box>
      </Box>
      <Box sx={{ display: 'flex', gap: 2, alignSelf: 'start' }}>
        {createdAt && (
          <Tooltip title={<T keyName="active-plan-subscribed-at-tooltip" />}>
            <Typography variant="caption">{formatDate(createdAt)}</Typography>
          </Tooltip>
        )}
      </Box>
    </Box>
  );
};
