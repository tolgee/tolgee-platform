import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { BillingPeriodType } from './PeriodSwitch';

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
  pricePerSeat: number;
  subscriptionPrice: number;
  period: BillingPeriodType;
};

export const PlanPrice: React.FC<Props> = ({
  pricePerSeat,
  subscriptionPrice,
  period,
}) => {
  return (
    <StyledPrice>
      {(() => {
        if (subscriptionPrice > 0 && pricePerSeat > 0) {
          return (
            <>
              <StyledPrimaryPrice>
                <T
                  keyName="billing-self-hosted-subscription-price"
                  params={{ price: subscriptionPrice }}
                />
              </StyledPrimaryPrice>
              <StyledSecondaryPrice>
                <T
                  keyName="billing-self-hosted-price-per-seat-extra"
                  params={{ price: pricePerSeat }}
                />
              </StyledSecondaryPrice>
            </>
          );
        } else if (subscriptionPrice > 0) {
          return (
            <StyledPrimaryPrice>
              <T
                keyName="billing-self-hosted-subscription-price-monthly"
                params={{ price: subscriptionPrice }}
              />
            </StyledPrimaryPrice>
          );
        } else if (pricePerSeat > 0) {
          return (
            <StyledPrimaryPrice>
              <T
                keyName="billing-self-hosted-price-per-seat"
                params={{ price: pricePerSeat }}
              />
            </StyledPrimaryPrice>
          );
        }
      })()}
      {period === 'YEARLY' && subscriptionPrice > 0 && (
        <StyledPeriod>
          <T keyName="billing_period_annual" />
        </StyledPeriod>
      )}
    </StyledPrice>
  );
};
