import { useEffect, useRef, useState } from 'react';
import { useMediaQuery } from '@mui/material';
import { useScrollStatus } from 'tg.component/common/useScrollStatus';

export function useHorizontalScroll(
  columnSizes: number[],
  containerWidth: number
) {
  const tableRef = useRef<HTMLDivElement>(null!);
  const verticalScrollRef = useRef<HTMLDivElement>(null!);

  const fullWidth = columnSizes.reduce((a, b) => a + b, 0);

  const [scrollLeft, scrollRight] = useScrollStatus(verticalScrollRef, [
    fullWidth,
    containerWidth,
  ]);

  const [tablePosition, setTablePosition] = useState({ left: 0, right: 0 });

  useEffect(() => {
    const position = tableRef.current?.getBoundingClientRect();
    if (position) {
      const left = position.left;
      const right = document.body.offsetWidth - position.right;
      setTablePosition({ left, right });
    }
  }, [tableRef.current, containerWidth]);

  const hasMinimalHeight = useMediaQuery('(min-height: 400px)');

  function handleHorizontalScroll(direction: 'left' | 'right') {
    const element = verticalScrollRef.current;
    if (element) {
      const position = element.scrollLeft;
      element.scrollTo({
        left: position + (direction === 'left' ? -350 : +350),
      });
    }
  }

  return {
    tableRef,
    verticalScrollRef,
    scrollLeft,
    scrollRight,
    tablePosition,
    hasMinimalHeight,
    handleHorizontalScroll,
  };
}
