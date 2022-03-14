import React, { useContext } from 'react';
import { useTopBarTrigger } from './useTopBarTrigger';

const TopBarContext = React.createContext(false);

export const TopBarProvider: React.FC = (props) => {
  const hidden = useTopBarTrigger();

  return (
    <TopBarContext.Provider value={hidden}>
      {props.children}
    </TopBarContext.Provider>
  );
};

export const useTopBarHidden = () => {
  return useContext(TopBarContext);
};
