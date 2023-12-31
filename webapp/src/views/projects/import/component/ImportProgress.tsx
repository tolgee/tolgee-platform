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
    background-color: ${({ theme }) => theme.palette.import.progressWorking};
    transition: width 1s steps(1, jump-end),
      background-color 1s steps(1, jump-end);
  }

  &.loading::before {
    width: 99%;
    transition: width 30s cubic-bezier(0.15, 0.735, 0.095, 1);
  }

  &.finish::before {
    width: 100%;
    background-color: ${({ theme }) => theme.palette.import.progressDone};
    transition: width 0.2s ease-in-out, background-color 0.2s steps(1, jump-end);
  }
`;

export const ImportProgressBar = (props: {
  loading: boolean;
  loaded: boolean;
}) => {
  const [transitionLoading, setTransitionLoading] = React.useState(false);

  useLoadingRegister(props.loading);

  useEffect(() => {
    setTimeout(() => {
      setTransitionLoading(true);
    }, 10);
  }, []);

  const classes = clsx({
    loading: transitionLoading && props.loading,
    finish: props.loaded,
  });

  return (
    <Box
      px={'200px'}
      sx={{ width: '100%', display: 'flex', alignItems: 'center' }}
    >
      <StyledProgress data-cy="import-progress" className={classes} />
    </Box>
  );
};
