import { Box, styled, SxProps } from '@mui/material';
import { T } from '@tolgee/react';
import { MtHint } from 'tg.component/billing/MtHint';
import { StringSlotsHint } from './Hints';
import { KeysHint, StringsHint } from 'tg.component/common/StringsHint';

export const IncludedItemContainer = styled(Box)``;

export const StyledQuantity = styled(Box)`
  display: inline;
`;

type IncludedItemProps = {
  count: number;
  sx?: SxProps;
  className?: string;
  highlightColor: string;
  'data-cy'?: string;
};

export const IncludedStringSlots = ({
  count,
  highlightColor,
  ...containerProps
}: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...containerProps}>
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
  ...containerProps
}: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...containerProps}>
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

export const IncludedKeys = ({
  count,
  highlightColor,
  ...containerProps
}: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...containerProps}>
      {count === -1 ? (
        <T
          keyName="billing_subscription_included_keys_negotiable"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <KeysHint />,
          }}
        />
      ) : (
        <T
          keyName="billing_subscription_included_keys"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            quantity: count,
            hint: <KeysHint />,
          }}
        />
      )}
    </IncludedItemContainer>
  );
};

export const IncludedCredits = ({
  count,
  highlightColor,
  ...containerProps
}: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...containerProps}>
      {count === -1 ? (
        <T
          keyName="billing_subscription_included_credits_negotiable"
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
  ...containerProps
}: IncludedItemProps) => {
  return (
    <IncludedItemContainer {...containerProps}>
      {count === -1 ? (
        <T
          keyName="billing_subscription_included_seats_negotiable"
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
