import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';

const StyledPrice = styled(Box)`
  grid-area: price;
  display: grid;
  min-height: 45px;
  align-items: start;
  font-size: 18px;
  color: ${({ theme }) => theme.palette.primary.main};
  align-content: end;
`;

type Props = {
  pricePerSeat: number;
  subscriptionPrice: number;
};

export const PlanPrice: React.FC<Props> = ({
  pricePerSeat,
  subscriptionPrice,
}) => {
  return (
    <StyledPrice>
      {subscriptionPrice > 0 ? (
        <T
          keyName="billing-self-hosted-ee-subscriptions-price-with-subscription-price"
          params={{ pricePerSeat, subscriptionPrice }}
        />
      ) : (
        <T
          keyName="billing-self-hosted-ee-subscriptions-price-without-subscription-price"
          params={{ pricePerSeat }}
        />
      )}
    </StyledPrice>
  );
};
