import { Box, styled } from '@mui/material';

const StyledContainer = styled(Box)`
  display: grid;
  border-radius: 4px;
  background: ${({ theme }) =>
    theme.palette.tokens._components.progressbar.background};
  overflow: hidden;
  transition: all 0.5s ease-in-out;
  position: relative;
  width: 100%;
`;

const StyledProgress = styled(Box)`
  border-radius: 4px;
  background: ${({ theme }) =>
    theme.palette.tokens._components.progressbar.task.inProgress};
  transition: all 0.5s ease-in-out;
  height: 8px;
`;

type Props = {
  max: number;
  progress: number;
};

export const BatchProgress = ({ max, progress }: Props) => {
  const percent = (progress / (max || 1)) * 100;
  return (
    <StyledContainer>
      <StyledProgress width={`${percent}%`} />
    </StyledContainer>
  );
};
