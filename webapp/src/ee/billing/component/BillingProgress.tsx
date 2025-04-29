import React from 'react';
import { Box, BoxProps, styled, Tooltip, useTheme } from '@mui/material';
import clsx from 'clsx';
import { T } from '@tolgee/react';

import { BILLING_CRITICAL_FRACTION } from './constants';
import { ProgressItem } from './getProgressData';

const DOT_SIZE = 8;

const StyledContainer = styled(Box)`
  display: grid;
  border-radius: 4px;
  background: ${({ theme }) =>
    theme.palette.tokens._components.progressbar.background};
  overflow: hidden;
  transition: all 0.5s ease-in-out;
  position: relative;
  &.over {
    background: transparent;
  }
`;

const StyledProgress = styled(Box)`
  border-radius: 4px;
  background: ${({ theme }) =>
    theme.palette.tokens._components.progressbar.pricing.sufficient};
  transition: all 0.5s ease-in-out;
  &.critical {
    background: ${({ theme }) =>
      theme.palette.tokens._components.progressbar.pricing.low};
  }
`;

const StyledExtra = styled(Box)`
  border-radius: 4px;
  position: absolute;
  right: 0px;
  top: 0px;
  bottom: 0px;
  background: ${({ theme }) =>
    theme.palette.tokens._components.progressbar.pricing.overForbidden};
  &.isPayAsYouGo {
    background: ${({ theme }) =>
      theme.palette.tokens._components.progressbar.pricing.over};
  }
`;

const StyledLabelContainer = styled('div')`
  margin: 8px;
  display: grid;
  grid-template-columns: auto auto 1fr 1fr 1fr;
  gap: 5px;
  align-items: center;
  white-space: nowrap;
`;

const StyledDot = styled(Box)`
  width: ${DOT_SIZE}px;
  height: ${DOT_SIZE}px;
  border-radius: ${DOT_SIZE / 2}px;
  filter: brightness(
    ${({ theme }) => (theme.palette.mode === 'dark' ? 0.8 : 1)}
  );
`;

type StatItem = {
  label: React.ReactNode;
  color: string;
};

type Props = BoxProps & {
  progressItem: ProgressItem;
  height?: number;
  isPayAsYouGo?: boolean;
  showLabels?: boolean;
};

export const BillingProgress: React.FC<Props> = ({
  progressItem,
  height = 6,
  isPayAsYouGo,
  showLabels,
  ...boxProps
}) => {
  const normalized =
    progressItem.used > progressItem.included
      ? progressItem.included
      : progressItem.used < 0
      ? 0
      : progressItem.used;
  const critical =
    normalized > BILLING_CRITICAL_FRACTION * progressItem.included &&
    !isPayAsYouGo;

  const theme = useTheme();

  const extra =
    progressItem.used > progressItem.included
      ? progressItem.used - progressItem.included
      : 0;

  const fullLength =
    progressItem.used > progressItem.included
      ? progressItem.used
      : progressItem.included;
  const progressLength = (normalized / fullLength) * 100;
  const extraProgressLength = (extra / fullLength) * 100;

  const labels: StatItem[] = [];

  if (progressItem.included - normalized) {
    labels.push({
      label: (
        <T
          keyName="billing-progress-label-included-in-plan"
          params={{ value: progressItem.included }}
        />
      ),
      color: theme.palette.tokens._components.progressbar.background,
    });
  }

  if (normalized) {
    labels.push({
      label: (
        <T
          keyName="billing-progress-label-used"
          params={{ value: normalized }}
        />
      ),
      color: critical
        ? theme.palette.tokens._components.progressbar.pricing.low
        : theme.palette.tokens._components.progressbar.pricing.sufficient,
    });
  }

  if (extra) {
    labels.push({
      label: (
        <T keyName="billing-progress-label-over" params={{ value: extra }} />
      ),
      color: isPayAsYouGo
        ? theme.palette.tokens._components.progressbar.pricing.over
        : theme.palette.tokens._components.progressbar.pricing.overForbidden,
    });
  }

  const tooltip = (
    <StyledLabelContainer>
      {labels.map(({ label, color }, i) => (
        <React.Fragment key={i}>
          <StyledDot gridColumn={1} bgcolor={color} />
          <Box data-cy="billing-progress-label-item">{label}</Box>
        </React.Fragment>
      ))}
    </StyledLabelContainer>
  );

  return (
    <Tooltip open={showLabels ? undefined : false} title={tooltip}>
      <StyledContainer
        className={clsx({ over: Boolean(extra) })}
        height={height}
        {...boxProps}
      >
        {Boolean(progressLength) && (
          <StyledProgress
            width={`${progressLength}%`}
            className={clsx({ critical })}
            minWidth={'6px'}
          />
        )}
        {Boolean(extra) && (
          <StyledExtra
            width={`${extraProgressLength}%`}
            className={clsx({ isPayAsYouGo })}
          />
        )}
      </StyledContainer>
    </Tooltip>
  );
};
