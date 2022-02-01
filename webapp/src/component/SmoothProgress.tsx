import { useState, useEffect } from 'react';
import clsx from 'clsx';
import { useDebounce } from 'use-debounce';
import { makeStyles } from '@material-ui/core';

const useStyles = makeStyles((theme) => ({
  progress: {
    height: 4,
    background: theme.palette.primary.light,
    width: 0,
  },
  loading: {
    transition: 'width 30s cubic-bezier(0.150, 0.735, 0.095, 1.0)',
  },
  finish: {
    height: 0,
    transition:
      'width 0.2s ease-in-out, height 0.5s cubic-bezier(0.930, 0.000, 0.850, 0.015)',
  },
}));

type Props = {
  loading: boolean;
  className?: string;
};

export const SmoothProgress: React.FC<Props> = ({ loading, className }) => {
  const classes = useStyles();
  const [stateLoading, setStateLoading] = useState(false);
  const [smoothedLoading] = useDebounce(stateLoading, 100);
  const [progress, setProgress] = useState(0);
  useEffect(() => {
    setStateLoading(Boolean(loading));
    if (loading === true) {
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
    <div
      data-cy="global-loading"
      style={{ width: `${progress === 1 ? 100 : progress * 95}%` }}
      className={clsx(
        {
          [classes.progress]: true,
          [classes.loading]: progress > 0 && progress < 1,
          [classes.finish]: progress === 1,
        },
        className
      )}
    />
  ) : null;
};
