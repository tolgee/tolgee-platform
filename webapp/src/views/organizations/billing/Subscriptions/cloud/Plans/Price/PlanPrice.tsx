import { Box, SxProps, styled } from '@mui/material';
import { BillingPeriodType, PeriodSwitch } from './PeriodSwitch';
import { components } from 'tg.service/apiSchema.generated';
import { PricePrimary, planIsPeriodDependant } from './PricePrimary';
import { PayAsYouGoPrices } from './PayAsYouGoPrices';

type PlanPricesModel = components['schemas']['PlanPricesModel'];

const StyledPrice = styled(Box)`
  display: grid;
`;

type Props = {
  prices: PlanPricesModel;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
  sx?: SxProps;
  className?: string;
};

export const PlanPrice: React.FC<Props> = ({
  prices,
  period,
  onPeriodChange,
  sx,
  className,
}) => {
  const needsPeriodSwitch = planIsPeriodDependant(prices);

  return (
    <StyledPrice {...{ sx, className }}>
      {needsPeriodSwitch && (
        <PeriodSwitch value={period} onChange={onPeriodChange} />
      )}
      <PricePrimary
        sx={{ justifySelf: 'center' }}
        period={period}
        prices={prices}
      />
      <PayAsYouGoPrices sx={{ paddingTop: '20px' }} prices={prices} />
    </StyledPrice>
  );
};
