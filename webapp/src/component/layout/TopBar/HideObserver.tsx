import { useEffect } from 'react';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useTopBarTrigger } from './useTopBarTrigger';

export const HideObserver = () => {
  const trigger = useTopBarTrigger();
  const { setTopBarHidden } = useGlobalActions();
  useEffect(() => {
    setTopBarHidden(trigger);
  }, [trigger]);

  useEffect(() => {
    return () => setTopBarHidden(false);
  }, []);

  return null;
};
