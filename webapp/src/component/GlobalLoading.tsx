import React, { useContext, useEffect, useState } from 'react';
import { styled } from '@mui/material';
import { SmoothProgress } from './SmoothProgress';

const LoadingContext = React.createContext({ loading: 0, spinners: 0 });
const LoadingSetterContext = React.createContext<{
  setLoading: React.Dispatch<React.SetStateAction<number>>;
  setSpinners: React.Dispatch<React.SetStateAction<number>>;
}>({
  setLoading: () => {},
  setSpinners: () => {},
});

const StyledSmoothProgress = styled(SmoothProgress)`
  position: fixed;
  z-index: ${({ theme }) => theme.zIndex.tooltip};
`;

export const GlobalLoading: React.FC = () => {
  const { loading, spinners } = useContext(LoadingContext);

  return (
    <StyledSmoothProgress loading={Boolean(loading && spinners === 0)} global />
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
