import * as React from 'react';
import { FC } from 'react';
import { Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { useDateFormatter } from 'tg.hooks/useLocale';

export interface SubscriptionPeriodInfoProps {
  isTrial: boolean;
  currentPeriodEnd?: number;
  currentBillingPeriod?: string;
  currentPeriodStart?: number;
}

export const SubscriptionPeriodInfo: FC<SubscriptionPeriodInfoProps> = ({
  isTrial,
  currentPeriodStart,
  currentPeriodEnd,
  currentBillingPeriod,
}) => {
  const formatDate = useDateFormatter();

  if (!currentPeriodStart || isTrial) {
    return (
      <Typography sx={{ fontStyle: 'italic' }}>
        <T keyName="admin_billing_no_period_info" />
      </Typography>
    );
  }

  return (
    <>
      <>
        <Typography variant={'body2'} sx={{ fontSize: '12px' }}>
          <T
            keyName="admin_billing_current_period"
            params={{
              period: currentBillingPeriod,
            }}
          />
        </Typography>

        <Typography variant={'body2'} sx={{ fontSize: '12px' }}>
          <T
            keyName="admin_billing_current_perdiod_start"
            params={{
              date: formatDate(currentPeriodStart),
            }}
          />
        </Typography>
        <Typography variant={'body2'} sx={{ fontSize: '12px' }}>
          <T
            keyName="admin_billing_current_perdiod_end"
            params={{
              date: formatDate(currentPeriodEnd),
            }}
          />
        </Typography>
      </>
    </>
  );
};
