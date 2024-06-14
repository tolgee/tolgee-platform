import { Box, SxProps, styled } from '@mui/material';
import { useMoneyFormatter } from 'tg.hooks/useLocale';
import { BillingPeriodType } from './PeriodSwitch';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';

type PlanPricesModel = components['schemas']['PlanPricesModel'];

const StyledPrimaryPrice = styled(Box)`
  font-size: 24px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledPeriod = styled('span')`
  font-size: 15px;
`;

export function planIsPeriodDependant(prices: PlanPricesModel | undefined) {
  return (
    prices && Boolean(prices.subscriptionYearly || prices.subscriptionMonthly)
  );
}

type Props = {
  prices: PlanPricesModel;
  period: BillingPeriodType;
  sx?: SxProps;
  className?: string;
};

export const PricePrimary = ({ prices, period, sx, className }: Props) => {
  const { subscriptionMonthly, subscriptionYearly } = prices;
  const formatMoney = useMoneyFormatter();

  const needsPeriodSwitch = planIsPeriodDependant(prices);

  const subscriptionPrice =
    period === 'MONTHLY' ? subscriptionMonthly : subscriptionYearly / 12;

  return (
    <StyledPrimaryPrice
      data-cy="billing-plan-monthly-price"
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
