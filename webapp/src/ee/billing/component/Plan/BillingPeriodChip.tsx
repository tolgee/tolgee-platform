import { Chip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/billingApiSchema.generated';

type BillingPeriod =
  components['schemas']['AdministrationBasicSubscriptionModel']['currentBillingPeriod'];

export const BillingPeriodChip = ({
  period,
}: {
  period?: BillingPeriod;
}) => {
  const { t } = useTranslate();
  const config: Record<NonNullable<BillingPeriod>, { label: string }> = {
    MONTHLY: { label: t('subscription_period_monthly') },
    YEARLY: { label: t('subscription_period_yearly') },
  };

  if (!period) return null;

  const periodConfig = config[period];
  if (!periodConfig) return null;

  return (
    <Chip
      size="small"
      variant="outlined"
      label={periodConfig.label}
      data-cy={`subscription-period-${period.toLowerCase()}`}
    />
  );
};
