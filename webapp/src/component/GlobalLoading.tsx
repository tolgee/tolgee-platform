import { makeStyles } from '@material-ui/core';
import React, { useContext, useState, useEffect } from 'react';
import { SmoothProgress } from './SmoothProgress';

const LoadingContext = React.createContext({ loading: 0, spinners: 0 });
const LoadingSetterContext = React.createContext<{
  setLoading: React.Dispatch<React.SetStateAction<number>>;
  setSpinners: React.Dispatch<React.SetStateAction<number>>;
}>({ setLoading: () => {}, setSpinners: () => {} });

const useStyles = makeStyles((theme) => ({
  loading: {
    position: 'fixed',
    zIndex: theme.zIndex.tooltip,
  },
}));

export const GlobalLoading: React.FC = () => {
  const classes = useStyles();
  const { loading, spinners } = useContext(LoadingContext);

  return (
    <SmoothProgress
      loading={Boolean(loading && spinners === 0)}
      className={classes.loading}
    />
  );
};

export const LoadingProvider: React.FC = (props) => {
  const [loading, setLoading] = useState(0);
  const [spinners, setSpinners] = useState(0);
  return (
    <LoadingContext.Provider value={{ loading, spinners }}>
      <LoadingSetterContext.Provider value={{ setLoading, setSpinners }}>
        {props.children}
      </LoadingSetterContext.Provider>
    </LoadingContext.Provider>
  );
};

export const useGlobalLoading = (loading: boolean | undefined) => {
  const { setLoading } = useContext(LoadingSetterContext);
  useEffect(() => {
    if (loading) {
      setLoading((num) => num + 1);
      return () => setLoading((num) => num - 1);
    }
  }, [loading]);
};

export const useLoadingRegister = (active = true) => {
  const { setSpinners } = useContext(LoadingSetterContext);
  useEffect(() => {
    if (active) {
      setSpinners((num) => num + 1);
      return () => setSpinners((num) => num - 1);
    }
  }, [active]);
};
