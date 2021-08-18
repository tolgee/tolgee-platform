import { useRef, useEffect } from 'react';

type Props = {
  callback: () => any;
  delay: number;
  enabled: boolean;
};

// resetable timer
export const useTimer = (props: Props) => {
  const timerRef = useRef<NodeJS.Timeout>();

  const clearTimer = () => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = undefined;
    }
  };

  // this starts timer and cancels previous
  const reStartTimer = () => {
    clearTimer();
    if (props.enabled) {
      timerRef.current = setTimeout(props.callback, props.delay);
    }
  };

  // clear timeout on unmount
  useEffect(() => {
    return clearTimer;
  }, [props.enabled]);

  useEffect(() => {
    // prevent overlay hanging,
    // when overlay is re-enabled or something
    clearTimer();
  }, [props.enabled]);

  return {
    reStartTimer,
    clearTimer,
  };
};
