import { useCallback, useEffect, useRef, useState } from 'react';
import { useDebouncedCallback } from 'use-debounce';
import { ReactList } from 'tg.component/reactList/ReactList';

export function useScrollCounter(
  reactListRef: React.RefObject<ReactList | null>,
  totalCount: number
) {
  const [scrollIndex, setScrollIndex] = useState(1);
  const [toolbarVisible, setToolbarVisible] = useState(false);
  const [listInstance, setListInstance] = useState(reactListRef.current);

  useEffect(() => {
    setListInstance(reactListRef.current);
  });

  const getVisibleRangeRef = useRef<(() => number[]) | undefined>();
  getVisibleRangeRef.current = listInstance?.getVisibleRange.bind(listInstance);

  const onScroll = useDebouncedCallback(
    () => {
      const [start, end] = getVisibleRangeRef.current?.() || [0, 0];
      const fromBeginning = start;
      const toEnd = totalCount - 1 - end;
      const total = fromBeginning + toEnd || 1;
      const progress = (total - toEnd) / total;
      const newIndex = Math.round(progress * (totalCount - 1) + 1);
      setScrollIndex(newIndex);
      setToolbarVisible(start > 0 && newIndex > 1);
    },
    100,
    { maxWait: 200 }
  );

  useEffect(() => {
    onScroll();
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, [listInstance]);

  const handleScrollUp = useCallback(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  return {
    scrollIndex,
    toolbarVisible,
    handleScrollUp,
  };
}
