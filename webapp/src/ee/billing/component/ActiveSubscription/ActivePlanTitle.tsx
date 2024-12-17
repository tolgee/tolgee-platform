import { T, useTranslate } from '@tolgee/react';
import { Box, Chip, Tooltip, Typography } from '@mui/material';

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
  nonCommercial: boolean;
};

export const ActivePlanTitle = ({
  name,
  createdAt,
  periodStart,
  periodEnd,
  highlightColor,
  nonCommercial,
}: Props) => {
  const formatDate = useDateFormatter();
  const { t } = useTranslate();

  return (
    <Box sx={{ mb: 1 }}>
      <Box display="flex" alignItems="center" gap={2}>
        <PlanTitleText sx={{ color: highlightColor, mb: 1 }}>
          {name}
        </PlanTitleText>
        {nonCommercial && (
          <Tooltip title={t('billing_plan_non_commercial_hint')}>
            <Chip
              sx={{ mt: -1 }}
              label={t('billing_plan_non_commercial_label')}
              size="small"
              color="success"
            />
          </Tooltip>
        )}
      </Box>
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
