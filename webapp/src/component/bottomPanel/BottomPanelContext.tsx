import React, { useContext, useState } from 'react';

const BottomPanelContext = React.createContext({ height: 0 });
const BottomPanelSettersContext = React.createContext<{
  setHeight: React.Dispatch<React.SetStateAction<number>>;
}>({ setHeight: () => {} });

export const BottomPanelProvider = ({ children }) => {
  const [height, setHeight] = useState(0);

  return (
    <BottomPanelContext.Provider value={{ height }}>
      <BottomPanelSettersContext.Provider value={{ setHeight }}>
        {children}
      </BottomPanelSettersContext.Provider>
    </BottomPanelContext.Provider>
  );
};

export const useBottomPanelSetters = () => {
  return useContext(BottomPanelSettersContext);
};

export const useBottomPanel = () => {
  return useContext(BottomPanelContext);
};
