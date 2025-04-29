import { Box, styled, SxProps } from '@mui/material';
import { T } from '@tolgee/react';
import { MtHint } from 'tg.component/billing/MtHint';
import { KeysHint, StringsHint } from 'tg.component/common/StringsHint';
import React, { ComponentProps, ReactNode } from 'react';

export const StyledQuantity = styled(Box)`
  display: inline;
`;

type IncludedItemProps = {
  /**
   * Number of included items. -1 for unlimited, -2 for negotiable
   */
  count: number;
  sx?: SxProps;
  className?: string;
  highlightColor: string;
  'data-cy'?: string;
};

export const IncludedStrings = ({
  count,
  highlightColor,
  ...containerProps
}: IncludedItemProps) => {
  return (
    <Container
      {...containerProps}
      count={count}
      negotiableLabel={
        <T
          keyName="billing_subscription_included_strings_negotiable"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <StringsHint />,
          }}
        />
      }
      unlimitedLabel={
        <T
          keyName="billing_subscription_included_strings_unlimited"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <StringsHint />,
          }}
        />
      }
      numberLabel={
        <T
          keyName="billing_subscription_included_strings"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            quantity: count,
            hint: <StringsHint />,
          }}
        />
      }
    />
  );
};

export const IncludedKeys = ({
  count,
  highlightColor,
  ...containerProps
}: IncludedItemProps) => {
  return (
    <Container
      {...containerProps}
      count={count}
      negotiableLabel={
        <T
          keyName="billing_subscription_included_keys_negotiable"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <KeysHint />,
          }}
        />
      }
      unlimitedLabel={
        <T
          keyName="billing_subscription_included_keys_unlimited"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <KeysHint />,
          }}
        />
      }
      numberLabel={
        <T
          keyName="billing_subscription_included_keys"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            quantity: count,
            hint: <KeysHint />,
          }}
        />
      }
    />
  );
};

export const IncludedCredits = ({
  count,
  highlightColor,
  ...containerProps
}: IncludedItemProps) => {
  return (
    <Container
      {...containerProps}
      count={count}
      negotiableLabel={
        <T
          keyName="billing_subscription_included_credits_negotiable"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <MtHint />,
          }}
        />
      }
      unlimitedLabel={
        <T
          keyName="billing_subscription_included_credits_unlimited"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <MtHint />,
          }}
        />
      }
      numberLabel={
        <T
          keyName="billing_subscription_included_credits"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            quantity: count,
            hint: <MtHint />,
          }}
        />
      }
    ></Container>
  );
};

export const IncludedSeats = ({
  count,
  highlightColor,
  ...containerProps
}: IncludedItemProps) => {
  return (
    <Container
      {...containerProps}
      count={count}
      negotiableLabel={
        <T
          keyName="billing_subscription_included_seats_negotiable"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <span />,
          }}
        />
      }
      unlimitedLabel={
        <T
          keyName="billing_subscription_included_seats_unlimited"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            hint: <span />,
          }}
        />
      }
      numberLabel={
        <T
          keyName="billing_subscription_included_seats"
          params={{
            highlight: <StyledQuantity color={highlightColor} />,
            quantity: count,
            hint: <span />,
          }}
        />
      }
    ></Container>
  );
};

const Container = ({
  count,
  negotiableLabel,
  unlimitedLabel,
  numberLabel,
  ...containerProps
}: ComponentProps<typeof Box> & {
  count: number;
  /**
   * Displayed when included usage is negotiable (-2) (Usually for negotiable enterprise plans)
   */
  negotiableLabel: ReactNode;
  /**
   * Displayed when usage is unlimited (-1)
   */
  unlimitedLabel: ReactNode;

  /**
   * Displayed when usage is a standard number
   */
  numberLabel: ReactNode;
}) => {
  if (count === -2) {
    return <Box {...containerProps}>{negotiableLabel}</Box>;
  }

  if (count === -1) {
    return <Box {...containerProps}>{unlimitedLabel}</Box>;
  }

  if (count >= 0) {
    return <Box {...containerProps}>{numberLabel}</Box>;
  }

  return null;
};
