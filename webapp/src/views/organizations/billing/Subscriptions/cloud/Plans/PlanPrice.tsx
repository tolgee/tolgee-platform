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
  font-size: 15px;
`;

const StyledPeriod = styled(Box)`
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  prices: PlanPricesModel;
  period: BillingPeriodType;
};

export const PlanPrice: React.FC<Props> = ({ prices, period }) => {
  const {
    subscriptionMonthly,
    subscriptionYearly,
    perSeat,
    perThousandMtCredits,
    perThousandTranslations,
  } = prices;
  const formatMoney = useMoneyFormatter();

  const differentPricesAnnualy = Boolean(
    subscriptionYearly || subscriptionMonthly
  );

  const subscriptionPrice =
    period === 'MONTHLY' ? subscriptionMonthly : subscriptionYearly / 12;

  return (
    <StyledPrice>
      <StyledPrimaryPrice>
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
        <StyledSecondaryPrice>
          <T
            keyName="billing-plan-price-per-seat-extra"
            params={{ price: perSeat }}
          />
        </StyledSecondaryPrice>
      )}
      {Boolean(perThousandTranslations) && (
        <StyledSecondaryPrice>
          <T
            keyName="billing-plan-price-per-thousand-translations-extra"
            params={{ price: perThousandTranslations }}
          />
        </StyledSecondaryPrice>
      )}
      {Boolean(perThousandMtCredits) && (
        <StyledSecondaryPrice>
          <T
            keyName="billing-plan-price-per-thousand-mt-credits-extra"
            params={{ price: perThousandMtCredits }}
          />
        </StyledSecondaryPrice>
      )}
      {period === 'YEARLY' && differentPricesAnnualy && (
        <StyledPeriod>
          <T keyName="billing_period_annual" />
        </StyledPeriod>
      )}
    </StyledPrice>
  );
};
