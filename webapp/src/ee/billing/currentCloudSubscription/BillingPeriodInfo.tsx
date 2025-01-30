import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { Box, styled } from '@mui/material';
import { components } from 'tg.service/billingApiSchema.generated';

const StyledPositive = styled('span')`
  color: ${({ theme }) => theme.palette.secondary.main};
`;

const StyledNegative = styled('span')`
  color: ${({ theme }) => theme.palette.error.main};
`;

export const BillingPeriodInfo: FC<{
  subscription: components['schemas']['CloudSubscriptionModel'];
}> = ({ subscription }) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();

  if (subscription.plan.free || subscription.status == 'TRIALING') {
    return null;
  }

  return (
    <>
      <Box gridColumn="1">{t('billing_actual_period')}</Box>
      <Box gridColumn="2 / -1" data-cy="billing-actual-period">
        {subscription.currentBillingPeriod === 'MONTHLY'
          ? t('billing_monthly')
          : t('billing_annual')}
      </Box>
      <Box gridColumn="1">{t('billing_actual_period_end')}</Box>
      <Box gridColumn="2 / -1" data-cy="billing-actual-period-end">
        {formatDate(subscription.currentPeriodEnd)} (
        {!subscription.cancelAtPeriodEnd ? (
          <StyledPositive>{t('billing_actual_period_renewal')}</StyledPositive>
        ) : (
          <StyledNegative>{t('billing_actual_period_finish')}</StyledNegative>
        )}
        )
      </Box>
    </>
  );
};
