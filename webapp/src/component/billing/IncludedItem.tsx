import { Box, SxProps, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { MtHint } from 'tg.component/billing/MtHint';
import { StringSlotsHint, StringsHint } from './Hints';

export const IncludedItemContainer = styled(Box)``;

export const StyledQuantity = styled(Box)`
  display: inline;
`;

type IncludedItemProps = {
  count: number;
  sx?: SxProps;
  className?: string;
  highlightColor: string;
};

export const IncludedStringSlots = ({
  count,
  highlightColor,
  sx,
  className,
}: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...{ sx, className }}>
      <T
        keyName="billing_subscription_included_slots_strings"
        params={{
          highlight: <StyledQuantity color={highlightColor} />,
          quantity: count,
          hint: <StringSlotsHint />,
        }}
      />
    </IncludedItemContainer>
  );
};

export const IncludedStrings = ({
  count,
  highlightColor,
  sx,
  className,
}: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...{ sx, className }}>
      {count === -1 ? (
        <T
          keyName="billing_subscription_included_strings_unlimited"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <StringsHint />,
          }}
        />
      ) : (
        <T
          keyName="billing_subscription_included_strings"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
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
  highlightColor,
  sx,
  className,
}: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...{ sx, className }}>
      {count === -1 ? (
        <T
          keyName="billing_subscription_included_credits_unlimited"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <MtHint />,
          }}
        />
      ) : (
        <T
          keyName="billing_subscription_included_credits"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            quantity: count,
            hint: <MtHint />,
          }}
        />
      )}
    </IncludedItemContainer>
  );
};

export const IncludedSeats = ({
  count,
  highlightColor,
  sx,
  className,
}: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...{ sx, className }}>
      {count === -1 ? (
        <T
          keyName="billing_subscription_included_seats_unlimited"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <span />,
          }}
        />
      ) : (
        <T
          keyName="billing_subscription_included_seats"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            quantity: count,
            hint: <span />,
          }}
        />
      )}
    </IncludedItemContainer>
  );
};
