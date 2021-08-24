import { makeStyles } from '@material-ui/core';
import React, { useContext, useState, useEffect } from 'react';
import { SmoothProgress } from './SmoothProgress';

const LoadingValueContext = React.createContext(0);
const LoadingSetterContext = React.createContext<
  React.Dispatch<React.SetStateAction<number>>
>(() => {});

const useStyles = makeStyles((theme) => ({
  loading: {
    position: 'fixed',
    zIndex: theme.zIndex.tooltip,
  },
}));

export const GlobalLoading: React.FC = () => {
  const classes = useStyles();
  const globalLoading = useContext(LoadingValueContext);

  return (
    <SmoothProgress
      loading={Boolean(globalLoading)}
      className={classes.loading}
    />
  );
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
