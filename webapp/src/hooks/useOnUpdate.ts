import { useEffect, useRef } from 'react';

export const useOnUpdate: typeof useEffect = (effect, deps) => {
  const initial = useRef(true);
  useEffect(() => {
    if (!initial.current) {
      effect();
    }
    initial.current = false;
  }, deps);
};
