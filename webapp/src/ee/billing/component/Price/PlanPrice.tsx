import { Box, styled, SxProps } from '@mui/material';
import { BillingPeriodType, PeriodSwitch } from './PeriodSwitch';
import { components } from 'tg.service/apiSchema.generated';
import { PricePrimary } from './PricePrimary';
import { PayAsYouGoPrices } from './PayAsYouGoPrices';
import { isPlanPeriodDependant } from '../Plan/plansTools';

type PlanPricesModel = components['schemas']['PlanPricesModel'];

const StyledPrice = styled(Box)`
  display: grid;
`;

type Props = {
  prices: PlanPricesModel;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
  highlightColor: string;
  sx?: SxProps;
  className?: string;
};

export const PlanPrice: React.FC<Props> = ({
  prices,
  period,
  onPeriodChange,
  highlightColor,
  sx,
  className,
}) => {
  const needsPeriodSwitch = isPlanPeriodDependant(prices);

  return (
    <StyledPrice {...{ sx, className }}>
      {needsPeriodSwitch && (
        <PeriodSwitch value={period} onChange={onPeriodChange} />
      )}
      <PricePrimary
        sx={{ justifySelf: 'center' }}
        period={period}
        prices={prices}
        highlightColor={highlightColor}
      />
      <PayAsYouGoPrices sx={{ paddingTop: '20px' }} prices={prices} />
    </StyledPrice>
  );
};
