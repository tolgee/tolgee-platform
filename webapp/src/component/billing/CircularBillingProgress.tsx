import clsx from 'clsx';
import { styled } from '@mui/material';

import { BILLING_CRITICAL_PERCENT } from './constants';

const RADIUS = 45;
const CIRCUIT = RADIUS * Math.PI * 2;

const StyledCircleBackground = styled('circle')`
  fill: none;
  stroke-width: 17px;
  stroke: ${({ theme }) => theme.palette.billingProgress.background};
  &.over {
    stroke: ${({ theme }) => theme.palette.billingProgress.sufficient};
  }
`;

const StyledCircleContent = styled('circle')`
  fill: none;
  stroke-width: 17px;
  stroke-linecap: round;
  transform: rotate(-90deg);
  transform-origin: 50% 50%;
  stroke-dasharray: ${CIRCUIT};
  stroke: ${({ theme }) => theme.palette.billingProgress.sufficient};
  &.critical {
    stroke: ${({ theme }) => theme.palette.billingProgress.low};
  }
`;

const StyledCircleContentOver = styled(StyledCircleContent)`
  stroke-width: 19px;
  stroke: ${({ theme }) => theme.palette.billingProgress.over};
  stroke-linecap: unset;
  &.separator {
    stroke-width: 17px;
    stroke: ${({ theme }) => theme.palette.billingProgress.separator};
  }
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
  const normalized = percent > 100 ? 100 : percent < 0 ? 0 : percent;
  const critical = normalized > BILLING_CRITICAL_PERCENT && !canGoOver;

  const strokeDashoffset = CIRCUIT - (normalized / 100) * CIRCUIT;
  const over = percent > 100 ? ((percent - 100) / percent) * 100 : null;
  const extraOffset = over && (over / 100) * CIRCUIT - CIRCUIT;

  return (
    <svg viewBox="0 0 114 114" style={{ width: size, height: size }}>
      <StyledCircleBackground
        className={clsx({ critical, over })}
        cx="57"
        cy="57"
        r={RADIUS}
      />
      {!over && (
        <StyledCircleContent
          className={clsx({ critical })}
          cx="57"
          cy="57"
          r={RADIUS}
          sx={{ strokeDashoffset }}
        />
      )}
      {extraOffset && (
        <>
          <StyledCircleContentOver
            cx="57"
            cy="57"
            r={RADIUS}
            sx={{ strokeDashoffset: extraOffset }}
          />
          <StyledCircleContentOver
            className="separator"
            cx="57"
            cy="57"
            r={RADIUS}
            sx={{
              strokeDashoffset: CIRCUIT - 10,
              transform: `rotate(${175 + extraOffset}deg)`,
            }}
          />
        </>
      )}
    </svg>
  );
};
