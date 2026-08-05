import { useEffect, useState } from 'react';

/** Holds `true` once seen, so a value that drives whether a control exists cannot make it disappear. */
export const useLatchedTrue = (value: boolean) => {
  const [latched, setLatched] = useState(false);
  useEffect(() => {
    if (value) {
      setLatched(true);
    }
  }, [value]);
  return value || latched;
};
