import { useEffect, useRef, useState } from 'react';

type TimerCountdownProps = {
  callback: () => any;
  delay: number;
  enabled: boolean;
};

export const useTimerCountdown = ({
  callback,
  delay,
  enabled,
}: TimerCountdownProps) => {
  const [remainingTime, setRemainingTime] = useState(delay);
  const timerRef = useRef<NodeJS.Timeout>();
  const intervalRef = useRef<NodeJS.Timeout>();

  const clearTimer = () => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = undefined;
    }
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = undefined;
    }
    setRemainingTime(delay);
  };

  const startTimer = () => {
    clearTimer();
    if (enabled) {
      setRemainingTime(delay);
      timerRef.current = setTimeout(() => {
        callback();
        clearTimer();
      }, delay);
      intervalRef.current = setInterval(() => {
        setRemainingTime((prevTime) => prevTime - 1000);
      }, 1000);
    }
  };

  useEffect(() => {
    return clearTimer;
  }, []);

  useEffect(() => {
    if (enabled) {
      startTimer();
    } else {
      clearTimer();
    }
  }, [enabled]);

  return {
    startTimer: startTimer,
    clearTimer,
    remainingTime,
  };
};
