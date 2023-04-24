import { Box, BoxProps, styled } from '@mui/material';
import clsx from 'clsx';

import { BILLING_CRITICAL_PERCENT } from './constants';

const StyledContainer = styled(Box)`
  display: grid;
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.billingProgress.background};
  overflow: hidden;
  transition: all 0.5s ease-in-out;
  position: relative;
`;

const StyledProgress = styled(Box)`
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.billingProgress.sufficient};
  transition: all 0.5s ease-in-out;
  &.critical {
    background: ${({ theme }) => theme.palette.billingProgress.low};
  }
`;

const StyledExtra = styled(Box)`
  position: absolute;
  right: 0px;
  top: 0px;
  bottom: 0px;
  background: ${({ theme }) => theme.palette.billingProgress.over};
  border-left: 2px solid
    ${({ theme }) => theme.palette.billingProgress.separator};
`;

type Props = BoxProps & {
  percent: number;
  height?: number;
  canGoOver?: boolean;
};

export const BillingProgress: React.FC<Props> = ({
  percent,
  height = 6,
  canGoOver,
  ...boxProps
}) => {
  const normalized = percent > 100 ? 100 : percent < 0 ? 0 : percent;
  const critical = normalized > BILLING_CRITICAL_PERCENT && !canGoOver;

  const extra = percent > 100 ? ((percent - 100) / percent) * 100 : null;

  return (
    <StyledContainer
      className={clsx({ critical })}
      height={height}
      {...boxProps}
    >
      <StyledProgress width={`${normalized}%`} className={clsx({ critical })} />
      {extra && <StyledExtra width={`${extra}%`} />}
    </StyledContainer>
  );
};
