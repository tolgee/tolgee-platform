import { useTranslate } from '@tolgee/react';
import { Tooltip } from '@mui/material';

import { StyledBillingHint } from 'tg.component/billing/Decorations';
import { FC } from 'react';

export const TrialRenewHint: FC<{ isTrialRenew: boolean }> = ({
  children,
  isTrialRenew,
}) => {
  const { t } = useTranslate();

  const hint = isTrialRenew
    ? t('billing_trial_renew_hint')
    : t('billing_trial_renew_hint');

  return (
    <Tooltip disableInteractive title={t('billing_trial_renew_hint')}>
      <StyledBillingHint>{children}</StyledBillingHint>
    </Tooltip>
  );
};
