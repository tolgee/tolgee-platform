import React, { useContext, useState } from 'react';
import { useEffect } from 'react';
import { makeStyles } from '@material-ui/core';
import { useDebounce } from 'use-debounce/lib';
import clsx from 'clsx';

const LoadingValueContext = React.createContext(0);
const LoadingSetterContext = React.createContext<
  React.Dispatch<React.SetStateAction<number>>
>(() => {});

const useStyles = makeStyles((theme) => ({
  progress: {
    position: 'fixed',
    zIndex: theme.zIndex.tooltip,
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

export const GlobalLoading: React.FC = () => {
  const classes = useStyles();
  const globalLoading = useContext(LoadingValueContext);

  const [progress, setProgress] = useState(0);

  const [loading] = useDebounce(globalLoading, 100);

  useEffect(() => {
    if (loading) {
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
  }, [loading]);

  return globalLoading || loading || progress ? (
    <div
      data-cy="global-loading"
      style={{ width: `${progress === 1 ? 100 : progress * 95}vw` }}
      className={clsx({
        [classes.progress]: true,
        [classes.loading]: progress > 0 && progress < 1,
        [classes.finish]: progress === 1,
      })}
    />
  ) : null;
};

export const LoadingProvider: React.FC = (props) => {
  const [loading, setLoading] = useState(0);
  return (
    <LoadingValueContext.Provider value={loading}>
      <LoadingSetterContext.Provider value={setLoading}>
        {props.children}
      </LoadingSetterContext.Provider>
    </LoadingValueContext.Provider>
  );
};

export const useGlobalLoading = (loading: boolean | undefined) => {
  const setLoading = useContext(LoadingSetterContext);
  useEffect(() => {
    if (loading) {
      setLoading((num) => num + 1);
      return () => setLoading((num) => num - 1);
    }
  }, [loading]);
};
