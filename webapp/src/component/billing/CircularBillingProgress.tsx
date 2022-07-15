import clsx from 'clsx';
import { styled } from '@mui/material';

import { BILLING_CRITICAL_PERCENT } from './constants';

const StyledContainer = styled('div')``;

const RADIUS = 45;
const CIRCUIT = RADIUS * Math.PI * 2;

const StyledSvg = styled('svg')`
  width: 28px;
  height: 28px;
`;

const StyledCircleBackground = styled('circle')`
  fill: none;
  stroke-width: 17px;
  stroke: ${({ theme }) => theme.palette.success.light}55;
  &.critical {
    stroke: ${({ theme }) => theme.palette.error.light}99;
  }
`;

const StyledCircleContent = styled('circle')`
  fill: none;
  stroke-width: 17px;
  stroke-linecap: round;
  transform: rotate(-90deg);
  transform-origin: 50% 50%;
  stroke-dasharray: ${CIRCUIT};
  stroke: ${({ theme }) => theme.palette.success.main};
  &.critical {
    stroke: ${({ theme }) => theme.palette.error.dark};
  }
`;

type Props = {
  percent: number;
};

export const CircularBillingProgress = ({ percent }: Props) => {
  const normalized = percent > 100 ? 100 : percent < 0 ? 0 : percent;
  const critical = normalized < BILLING_CRITICAL_PERCENT;
  const strokeDashoffset = CIRCUIT - (normalized / 100) * CIRCUIT;
  return (
    <StyledContainer>
      <StyledSvg viewBox="0 0 114 114">
        <StyledCircleBackground
          className={clsx({ critical })}
          cx="57"
          cy="57"
          r={RADIUS}
        />
        <StyledCircleContent
          className={clsx({ critical })}
          cx="57"
          cy="57"
          r={RADIUS}
          sx={{ strokeDashoffset }}
        />
      </StyledSvg>
    </StyledContainer>
  );
};
