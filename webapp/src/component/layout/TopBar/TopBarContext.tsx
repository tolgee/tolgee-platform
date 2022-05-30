import React, { useContext } from 'react';
import { useTopBarTrigger } from './useTopBarTrigger';

const TopBarContext = React.createContext(false);

export const TopBarProvider = ({ children }) => {
  const hidden = useTopBarTrigger();

  return (
    <TopBarContext.Provider value={hidden}>{children}</TopBarContext.Provider>
  );
};

export const useTopBarHidden = () => {
  return useContext(TopBarContext);
};
