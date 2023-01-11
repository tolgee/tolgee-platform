import { useEffect, useRef, useState } from 'react';

const MOVE_BACK_TRESHOLD = 50;

export const useTopBarTrigger = () => {
  const [triggered, setTriggered] = useState(false);

  const handlerRef = useRef<(byUser: boolean) => any>();
  const lastPosition = useRef(0);
  const triggeredByUser = useRef(false);

  handlerRef.current = () => {
    const currentScrollPos = window.pageYOffset;
    const difference = currentScrollPos - lastPosition.current;
    if (currentScrollPos < 80) {
      // we are at the top
      setTriggered(false);
    } else if (difference > 0) {
      // any movement down
      setTriggered(true);
    } else if (difference < -MOVE_BACK_TRESHOLD && triggeredByUser.current) {
      // user scrolling up
      setTriggered(false);
    }
  };

  useEffect(() => {
    const timer = setInterval(() => {
      lastPosition.current = window.pageYOffset;
      triggeredByUser.current = false;
    }, 1000);
    return () => clearInterval(timer);
  }, [lastPosition, triggeredByUser]);

  useEffect(() => {
    const handler = () => handlerRef.current!(false);
    window.addEventListener('scroll', handler, { passive: true });
    return () => window.removeEventListener('scroll', handler);
  }, [handlerRef]);

  useEffect(() => {
    const handler = () => {
      triggeredByUser.current = true;
    };
    window.addEventListener('wheel', handler, { passive: true });
    return () => window.removeEventListener('mousewheel', handler);
  }, [triggeredByUser]);

  useEffect(() => {
    const handler = () => {
      triggeredByUser.current = true;
    };
    window.addEventListener('touchmove', handler, { passive: true });
    return () => window.removeEventListener('touchmove', handler);
  }, [triggeredByUser]);

  return triggered;
};
