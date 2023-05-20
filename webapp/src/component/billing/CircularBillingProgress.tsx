import clsx from 'clsx';
import { styled } from '@mui/material';

import { BILLING_CRITICAL_FRACTION } from './constants';

const RADIUS = 45;
const CIRCUIT = RADIUS * Math.PI * 2;

const StyledCircleBackground = styled('circle')`
  fill: none;
  stroke-width: 17px;
  stroke: ${({ theme }) => theme.palette.billingProgress.background};
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
  stroke: ${({ theme }) => theme.palette.billingProgress.sufficient};
  &.critical {
    stroke: ${({ theme }) => theme.palette.billingProgress.low};
  }
`;

const StyledCircleContentOver = styled(StyledCircleContent)`
  stroke-width: 17px;
  stroke: ${({ theme }) => theme.palette.billingProgress.over};
  stroke-linecap: unset;
  stroke-linecap: round;
`;

type Props = {
  percent: number;
  size?: number;
  canGoOver?: boolean;
};

export const CircularBillingProgress = ({
  percent,
  size = 28,
  canGoOver,
}: Props) => {
  const normalized = percent > 1 ? 1 : percent < 0 ? 0 : percent;
  const critical = normalized > BILLING_CRITICAL_FRACTION && !canGoOver;

  const extra = percent > 1 ? percent - 1 : 0;

  const fullLength = percent > 1 ? percent : 1;
  let progressLength = CIRCUIT - (normalized / fullLength) * CIRCUIT;
  let extraProgressLength = (extra / fullLength) * CIRCUIT - CIRCUIT;
  let rotation = 0;

  if (extra) {
    // make bars separated
    progressLength += 20;
    extraProgressLength -= 20;
    rotation = 12.5;
  }

  return (
    <svg viewBox="0 0 114 114" style={{ width: size, height: size }}>
      <StyledCircleBackground
        className={clsx({ critical, extra })}
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
            transform: `rotate(${-90 - rotation}deg)`,
          }}
        />
      )}
    </svg>
  );
};
