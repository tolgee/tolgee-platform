import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useMoneyFormatter } from 'tg.hooks/useLocale';
import { BillingPeriodType } from './PeriodSwitch';

const StyledWrapper = styled(Box)`
  grid-area: price;
  display: grid;
  min-height: 45px;
  align-items: end;
`;

const StyledPrice = styled(Box)`
  font-size: 18px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledPeriod = styled(Box)`
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  price: number;
  period: BillingPeriodType;
};

export const PlanPrice: React.FC<Props> = ({ price, period }) => {
  const formatMoney = useMoneyFormatter();
  const t = useTranslate();

  return (
    <StyledWrapper>
      {price === 0 ? (
        <StyledPrice>{formatMoney(price)}</StyledPrice>
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
