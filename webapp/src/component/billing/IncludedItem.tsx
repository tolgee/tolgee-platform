import { Box, SxProps, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { MtHint } from 'tg.component/billing/MtHint';
import { StringsHint } from './StringsHint';

export const IncludedItemContainer = styled(Box)``;

export const StyledQuantity = styled('span')`
  color: ${({ theme }) => theme.palette.primary.main};
`;

type IncludedItemProps = {
  count: number;
  sx?: SxProps;
  className?: string;
};

export const IncludedStrings = ({
  count,
  sx,
  className,
}: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...{ sx, className }}>
      {count === -1 ? (
        <T
          keyName="billing_subscription_included_strings_unlimited"
          params={{
            highlight: <StyledQuantity />,
            hint: <StringsHint />,
          }}
        />
      ) : (
        <T
          keyName="billing_subscription_included_strings"
          params={{
            highlight: <StyledQuantity />,
            quantity: count,
            hint: <StringsHint />,
          }}
        />
      )}
    </IncludedItemContainer>
  );
};

export const IncludedCreadits = ({
  count,
  sx,
  className,
}: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...{ sx, className }}>
      {count === -1 ? (
        <T
          keyName="billing_subscription_included_credits_unlimited"
          params={{
            highlight: <StyledQuantity />,
            hint: <MtHint />,
          }}
        />
      ) : (
        <T
          keyName="billing_subscription_included_credits"
          params={{
            highlight: <StyledQuantity />,
            quantity: count,
            hint: <MtHint />,
          }}
        />
      )}
    </IncludedItemContainer>
  );
};

export const IncludedSeats = ({ count, sx, className }: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...{ sx, className }}>
      {count === -1 ? (
        <T
          keyName="billing_subscription_included_seats_unlimited"
          params={{
            highlight: <StyledQuantity />,
            hint: <span />,
          }}
        />
      ) : (
        <T
          keyName="billing_subscription_included_seats"
          params={{
            highlight: <StyledQuantity />,
            quantity: count,
            hint: <span />,
          }}
        />
      )}
    </IncludedItemContainer>
  );
};
