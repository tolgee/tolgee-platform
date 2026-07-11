import { useTestClock } from 'tg.service/useTestClock';

export const useCurrentDate = () => {
  const testClock = useTestClock();

  if (!testClock) {
    return new Date();
  }

  return new Date(testClock);
};
