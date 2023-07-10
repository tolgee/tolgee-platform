import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { BillingPeriodType } from './PeriodSwitch';
import { components } from 'tg.service/apiSchema.generated';
import { useMoneyFormatter } from 'tg.hooks/useLocale';

type PlanPricesModel = components['schemas']['PlanPricesModel'];

const StyledPrice = styled(Box)`
  grid-area: price;
  display: grid;
  min-height: 45px;
  align-items: start;
  align-content: end;
`;

const StyledPrimaryPrice = styled(Box)`
  font-size: 18px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledSecondaryPrice = styled(Box)`
  font-size: 13px;
`;

const StyledPeriod = styled(Box)`
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  prices: PlanPricesModel;
  period: BillingPeriodType;
};

export function planIsPeriodDependant(prices: PlanPricesModel) {
  const { subscriptionMonthly, subscriptionYearly } = prices;

  return Boolean(subscriptionYearly || subscriptionMonthly);
}

export const PlanPrice: React.FC<Props> = ({ prices, period }) => {
  const {
    subscriptionMonthly,
    subscriptionYearly,
    perSeat,
    perThousandMtCredits,
    perThousandTranslations,
  } = prices;
  const formatMoney = useMoneyFormatter();

  const differentPricesAnnualy = planIsPeriodDependant(prices);

  const subscriptionPrice =
    period === 'MONTHLY' ? subscriptionMonthly : subscriptionYearly / 12;

  return (
    <StyledPrice>
      <StyledPrimaryPrice data-cy="billing-plan-monthly-price">
        {subscriptionPrice === 0 ? (
          formatMoney(subscriptionPrice)
        ) : (
          <T
            keyName="billing-plan-monthly-price"
            params={{ price: subscriptionPrice }}
          />
        )}
      </StyledPrimaryPrice>
      {Boolean(perSeat) && (
        <StyledSecondaryPrice data-cy="billing-plan-price-per-seat-extra">
          <T
            keyName="billing-plan-price-per-seat-extra"
            params={{ price: perSeat }}
          />
        </StyledSecondaryPrice>
      )}
      {Boolean(perThousandTranslations) && (
        <StyledSecondaryPrice data-cy="billing-plan-price-per-thousand-strings-extra">
          <T
            keyName="billing-plan-price-per-thousand-strings-extra"
            params={{ price: perThousandTranslations }}
          />
        </StyledSecondaryPrice>
      )}
      {Boolean(perThousandMtCredits) && (
        <StyledSecondaryPrice data-cy="billing-plan-price-per-thousand-mt-credits-extra">
          <T
            keyName="billing-plan-price-per-thousand-mt-credits-extra"
            params={{ price: perThousandMtCredits }}
          />
        </StyledSecondaryPrice>
      )}
      {period === 'YEARLY' && differentPricesAnnualy && (
        <StyledPeriod data-cy="billing_period_annual">
          <T keyName="billing_period_annual" />
        </StyledPeriod>
      )}
    </StyledPrice>
  );
};
