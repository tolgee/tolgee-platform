import { useTranslate } from '@tolgee/react';
import { Box, Chip, Tooltip } from '@mui/material';

import { components } from 'tg.service/billingApiSchema.generated';
import { PlanTitleText } from '../Plan/PlanTitle';
import { ActivePlanDates } from './ActivePlanDates';

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
      <ActivePlanDates
        createdAt={createdAt}
        periodStart={periodStart}
        periodEnd={periodEnd}
      />
    </Box>
  );
};
