import { Box, styled } from '@mui/material';
import clsx from 'clsx';
import React, { useEffect } from 'react';
import { useLoadingRegister } from 'tg.component/GlobalLoading';

const StyledProgress = styled('div')<{ loading?: string; finish?: string }>`
  height: 4px;
  width: 100%;
  border-radius: 2px;
  background: ${({ theme }) => theme.palette.import.progressBackground};
  position: relative;

  &::before {
    content: '';
    height: 100%;
    width: 0%;
    position: absolute;
    top: 0;
    left: 0;
  }

  &.loading::before {
    width: 99%;
    background: ${({ theme }) => theme.palette.import.progressWorking};
    transition: width 30s cubic-bezier(0.15, 0.735, 0.095, 1);
  }

  &.finish::before {
    width: 100%;
    background: ${({ theme }) => theme.palette.import.progressDone};
    transition: width 0.2s ease-in-out;
  }
`;

export const ImportProgressBar = (props: { loading: boolean }) => {
  const [transitionLoading, setTransitionLoading] = React.useState(false);

  useLoadingRegister(props.loading);

  useEffect(() => {
    setTimeout(() => {
      setTransitionLoading(true);
    }, 100);
  }, []);

  return (
    <Box
      px={'200px'}
      sx={{ width: '100%', display: 'flex', alignItems: 'center' }}
    >
      <StyledProgress
        data-cy="import-progress"
        className={clsx({
          loading: transitionLoading && props.loading,
          finish: !props.loading,
        })}
      />
    </Box>
  );
};
