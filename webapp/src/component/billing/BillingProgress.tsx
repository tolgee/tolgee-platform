import { Box, styled } from '@mui/material';

const StyledContainer = styled(Box)`
  display: grid;
  height: 6px;
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.emphasis[300]};
`;

const StyledProgress = styled(Box)`
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.success.main};
`;

type Props = {
  percent: number;
};

export const BillingProgress: React.FC<Props> = ({ percent }) => {
  const normalized = percent > 100 ? 100 : percent < 0 ? 0 : percent;
  return (
    <StyledContainer>
      <StyledProgress width={`${normalized}%`} />
    </StyledContainer>
  );
};
