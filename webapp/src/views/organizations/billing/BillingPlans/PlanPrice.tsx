import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useMoneyFormatter } from 'tg.hooks/useLocale';
import { BillingPeriodType } from './PeriodSwitch';

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

type Props = {
  price: number;
  period: BillingPeriodType;
};

export const PlanPrice: React.FC<Props> = ({ price, period }) => {
  const formatter = useMoneyFormatter();
  const t = useTranslate();

  return (
    <StyledWrapper>
      {price === 0 ? (
        <StyledPrice>{formatter(price)}</StyledPrice>
      ) : (
        <>
          <StyledPrice>
            {t('billing_period_monthly_price', { price })}
          </StyledPrice>
          {period === 'YEARLY' && (
            <StyledPeriod>{t('billing_period_annual')}</StyledPeriod>
          )}
        </>
      )}
    </StyledWrapper>
  );
};
