import { PricePrimary } from 'tg.ee.module/billing/component/Price/PricePrimary';
import { styled, useTheme } from '@mui/material';
import { components } from 'tg.service/billingApiSchema.generated';

type PlanPricesModel = components['schemas']['PlanPricesModel'];
export type BillingPeriodType =
  components['schemas']['CloudSubscribeRequest']['period'];

const StyledPricePrimary = styled(PricePrimary)<{ bold?: boolean }>`
  font-size: 15px;
  font-weight: ${({ bold }) => (bold ? 'bold' : 'inherit')};
`;

export const PlanListPriceInfo = ({
  prices,
  period,
  bold,
}: {
  prices: PlanPricesModel;
  period: BillingPeriodType;
  bold?: boolean;
}) => {
  const theme = useTheme();
  return (
    <StyledPricePrimary
      prices={prices}
      period={period}
      highlightColor={theme.palette.primaryText}
      bold={bold}
    />
  );
};
