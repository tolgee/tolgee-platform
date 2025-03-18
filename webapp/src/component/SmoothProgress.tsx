import { useEffect, useState } from 'react';
import { useDebounce } from 'use-debounce';
import { styled, SxProps, useTheme } from '@mui/material';
import clsx from 'clsx';
import { useLoadingRegister } from './GlobalLoading';

const StyledProgress = styled('div')<{ loading?: string; finish?: string }>`
  height: 4px;
  background: ${({ theme }) => theme.palette.globalLoading.main};
  width: 0px;

  &.loading {
    transition: width 30s cubic-bezier(0.15, 0.735, 0.095, 1);
  }

  &.finish {
    height: 0px;
    transition: width 0.2s ease-in-out,
      height 0.5s cubic-bezier(0.93, 0, 0.85, 0.015);
  }
`;

type Props = {
  loading: boolean;
  className?: string;
  sx?: SxProps;
  global?: boolean;
};

export const SmoothProgress: React.FC<Props> = ({
  loading,
  className,
  sx,
  global,
}) => {
  const theme = useTheme();
  const [stateLoading, setStateLoading] = useState(false);
  const [smoothedLoading] = useDebounce(stateLoading, 100);
  const [progress, setProgress] = useState(0);
  useLoadingRegister(!global && loading);
  useEffect(() => {
    setStateLoading(Boolean(loading));
    if (loading) {
      setProgress(0);
    }
  }, [loading]);

  useEffect(() => {
    if (smoothedLoading) {
      setProgress((p) => (p === 0 ? 0.99 : 0));
      const timer = setTimeout(() => setProgress(0.99), 0);
      return () => {
        clearTimeout(timer);
        setProgress(0.99);
      };
    } else {
      setProgress((p) => (p !== 0 ? 1 : 0));
      const timer = setTimeout(() => setProgress(0), 1000);
      return () => {
        clearTimeout(timer);
        setProgress(0);
      };
    }
  }, [smoothedLoading]);

  return loading || smoothedLoading || progress ? (
    <StyledProgress
      data-cy="global-loading"
      style={{
        width: `${progress === 1 ? 100 : progress * 95}%`,
        background: global
          ? theme.palette.globalLoading.main
          : theme.palette.primary.main,
      }}
      className={clsx(
        {
          loading: progress > 0 && progress < 1,
          finish: progress === 1,
        },
        className
      )}
      sx={sx}
    />
  ) : null;
};
