import { RefObject, useEffect, useState } from 'react';

export const useScrollStatus = (
  ref: RefObject<HTMLDivElement>,
  deps?: React.DependencyList | undefined
) => {
  const [offsets, setOffests] = useState<[boolean, boolean]>([false, false]);
  const [recalculateScrollOffsets] = useState(() => () => {
    const element = ref.current;
    if (element) {
      const scrollLeft = element?.scrollLeft;
      const scrollWidth = element?.scrollWidth;
      const offsetWidth = element?.offsetWidth;
      const isOffsetLeft = scrollLeft !== 0;
      const isOffestRight = scrollLeft + offsetWidth < scrollWidth - 1;
      setOffests((current) => {
        const [oLeft, oRight] = current;
        if (oLeft !== isOffsetLeft || oRight !== isOffestRight) {
          return [isOffsetLeft, isOffestRight] as const;
        }
        return current;
      });
    }
  });

  useEffect(() => {
    ref.current?.addEventListener('scroll', recalculateScrollOffsets);
    return () =>
      ref.current?.removeEventListener('scroll', recalculateScrollOffsets);
  }, [ref]);

  useEffect(() => {
    addEventListener('resize', recalculateScrollOffsets);
    return () => removeEventListener('resize', recalculateScrollOffsets);
  }, [ref]);

  useEffect(() => {
    recalculateScrollOffsets();
  }, deps);

  return offsets;
};
