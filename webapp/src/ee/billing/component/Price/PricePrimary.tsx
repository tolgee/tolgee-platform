import { Box, styled, SxProps } from '@mui/material';
import { T } from '@tolgee/react';
import { useMoneyFormatter } from 'tg.hooks/useLocale';

import { BillingPeriodType } from './PeriodSwitch';
import { PlanType } from '../Plan/types';
import { isPlanPeriodDependant } from '../Plan/plansTools';

type PlanPricesModel = NonNullable<PlanType['prices']>;

const StyledPrimaryPrice = styled(Box)`
  font-size: 24px;
`;

const StyledPeriod = styled('span')`
  font-size: 15px;
`;

type Props = {
  prices: PlanPricesModel;
  period: BillingPeriodType;
  highlightColor: string;
  sx?: SxProps;
  className?: string;
  noPeriodSwitch?: boolean;
};

export const PricePrimary = ({
  prices,
  period,
  highlightColor,
  sx,
  className,
  noPeriodSwitch = false,
}: Props) => {
  const { subscriptionMonthly, subscriptionYearly } = prices;
  const formatMoney = useMoneyFormatter();

  const needsPeriodSwitch = !noPeriodSwitch && isPlanPeriodDependant(prices);

  const subscriptionPrice =
    period === 'MONTHLY' ? subscriptionMonthly : subscriptionYearly / 12;

  return (
    <StyledPrimaryPrice
      data-cy="billing-plan-monthly-price"
      color={highlightColor}
      {...{ sx, className }}
    >
      {subscriptionPrice === 0 ? (
        formatMoney(subscriptionPrice)
      ) : (
        <T
          keyName="billing-plan-monthly-price"
          params={{ price: subscriptionPrice }}
        />
      )}
      {period === 'YEARLY' && needsPeriodSwitch && (
        <StyledPeriod data-cy="billing_period_annual">
          {' '}
          (<T keyName="billing_period_annual" />)
        </StyledPeriod>
      )}
    </StyledPrimaryPrice>
  );
};
