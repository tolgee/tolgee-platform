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

export const TrialInfo: FC<{
  subscription: components['schemas']['CloudSubscriptionModel'];
}> = ({ subscription }) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();

  if (subscription.status != 'TRIALING') {
    return null;
  }

  return (
    <>
      <Box gridColumn="1">{t('billing_actual_trial_end')}</Box>
      <Box gridColumn="2 / -1" data-cy="billing-actual-period-end">
        {formatDate(subscription.trialEnd, {
          dateStyle: 'long',
          timeStyle: 'short',
        })}
      </Box>
    </>
  );
};
