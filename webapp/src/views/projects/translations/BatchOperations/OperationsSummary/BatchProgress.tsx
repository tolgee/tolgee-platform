import { Box, styled } from '@mui/material';
import clsx from 'clsx';

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

  &.done {
    background: ${({ theme }) =>
      theme.palette.tokens._components.progressbar.task.done};
  }
`;

type Props = {
  max: number;
  progress: number;
};

export const BatchProgress = ({ max, progress }: Props) => {
  const percent = (progress / (max || 1)) * 100;
  return (
    <StyledContainer>
      <StyledProgress
        className={clsx({ done: percent === 100 })}
        data-cy="batch-progress"
        data-cy-progress={percent}
        width={`${percent}%`}
      />
    </StyledContainer>
  );
};
