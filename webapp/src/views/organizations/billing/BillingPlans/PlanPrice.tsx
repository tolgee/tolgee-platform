import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useMoneyFormatter } from 'tg.hooks/useLocale';
import { BillingPeriodType } from './PeriodSelect';

type Props = {
  price: number;
  period: BillingPeriodType;
};

const StyledWrapper = styled(Box)`
  grid-area: price;
  display: flex;
  gap: 12px;
  align-items: baseline;
  color: ${({ theme }) => theme.palette.primary.main};
  align-self: center;
`;

const StyledPrice = styled(Box)`
  font-size: 18px;
`;

const StyledPeriod = styled(Box)`
  font-size: 14px;
`;

export const PlanPrice: React.FC<Props> = ({ price, period }) => {
  const formatter = useMoneyFormatter();
  const t = useTranslate();
  const periodLabel =
    period === 'MONTHLY'
      ? t('billing_period_price_per_month')
      : t('billing_period_price_per_year');
  return (
    <StyledWrapper>
      <StyledPrice>{formatter(price)}</StyledPrice>
      <StyledPeriod>{periodLabel}</StyledPeriod>
    </StyledWrapper>
  );
};
