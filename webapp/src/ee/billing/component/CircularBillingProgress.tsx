import clsx from 'clsx';
import { styled } from '@mui/material';

import { BILLING_CRITICAL_FRACTION } from './constants';

const RADIUS = 45;
const CIRCUIT = RADIUS * Math.PI * 2;

const StyledCircleBackground = styled('circle')`
  fill: none;
  stroke-width: 17px;
  stroke: ${({ theme }) =>
    theme.palette.tokens._components.progressbar.background};
  &.extra {
    stroke: transparent;
  }
`;

const StyledCircleContent = styled('circle')`
  fill: none;
  stroke-width: 17px;
  stroke-linecap: round;
  transform-origin: 50% 50%;
  stroke-dasharray: ${CIRCUIT};
  stroke: ${({ theme }) =>
    theme.palette.tokens._components.progressbar.pricing.sufficient};
  &.critical {
    stroke: ${({ theme }) =>
      theme.palette.tokens._components.progressbar.pricing.low};
  }
`;

const StyledCircleContentOver = styled(StyledCircleContent)`
  stroke-width: 17px;
  stroke-linecap: unset;
  stroke-linecap: round;
  stroke: ${({ theme }) =>
    theme.palette.tokens._components.progressbar.pricing.overForbidden};
  &.canGoOver {
    stroke: ${({ theme }) =>
      theme.palette.tokens._components.progressbar.pricing.over};
  }
`;

type Props = {
  value: number;
  maxValue: number;
  isPayAsYouGo: boolean;
  size?: number;
};

export const CircularBillingProgress = ({
  value,
  maxValue = 100,
  isPayAsYouGo,
  size = 28,
}: Props) => {
  const normalized = value > maxValue ? maxValue : value < 0 ? 0 : value;
  const critical =
    normalized > BILLING_CRITICAL_FRACTION * maxValue && !isPayAsYouGo;

  const extra = value > maxValue ? value - maxValue : 0;

  const fullLength = value > maxValue ? value : maxValue;
  let progressLength = CIRCUIT - (normalized / fullLength) * CIRCUIT;
  let extraProgressLength = CIRCUIT - (extra / fullLength) * CIRCUIT;
  let rotation = 0;

  if (extra) {
    // make bars separated
    progressLength += 20;
    extraProgressLength += 20;
    rotation = 12;
  }

  return (
    <svg viewBox="0 0 114 114" style={{ width: size, height: size }}>
      <StyledCircleBackground
        className={clsx({ extra })}
        cx="57"
        cy="57"
        r={RADIUS}
      />
      <StyledCircleContent
        className={clsx({ critical })}
        cx="57"
        cy="57"
        r={RADIUS}
        sx={{
          strokeDashoffset: progressLength,
          transform: `rotate(${-90 + rotation}deg)`,
        }}
      />
      {extra && (
        <StyledCircleContentOver
          cx="57"
          cy="57"
          r={RADIUS}
          sx={{
            strokeDashoffset: extraProgressLength,
            transform: `scale(1, -1) rotate(${90 + rotation}deg)`,
          }}
          className={clsx({ isPayAsYouGo: isPayAsYouGo })}
        />
      )}
    </svg>
  );
};
