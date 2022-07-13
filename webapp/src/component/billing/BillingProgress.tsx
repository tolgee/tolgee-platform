import { Box, BoxProps, styled } from '@mui/material';
import clsx from 'clsx';

const StyledContainer = styled(Box)`
  display: grid;
  height: 6px;
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.success.light}55;
  overflow: hidden;
  &.critical {
    background: ${({ theme }) => theme.palette.error.light}99;
  }
`;

const StyledProgress = styled(Box)`
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.success.main};
  &.critical {
    background: ${({ theme }) => theme.palette.error.dark};
  }
`;

type Props = BoxProps & {
  percent: number;
  treshold?: number;
};

export const BillingProgress: React.FC<Props> = ({
  percent,
  treshold = 10,
  ...boxProps
}) => {
  const normalized = percent > 100 ? 100 : percent < 0 ? 0 : percent;
  const critical = normalized < treshold;
  return (
    <StyledContainer className={clsx({ critical })} {...boxProps}>
      <StyledProgress width={`${normalized}%`} className={clsx({ critical })} />
    </StyledContainer>
  );
};
