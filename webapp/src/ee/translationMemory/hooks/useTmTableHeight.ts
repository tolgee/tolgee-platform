import { useCallback, useEffect, useRef, useState } from 'react';
import { useTheme } from '@mui/material';
import { useResizeObserver } from 'usehooks-ts';

/**
 * Owns the resize-observer plumbing that keeps the entries-list table sized to the available
 * vertical space (window bottom minus the table's top offset, minus a small spacing buffer).
 *
 * Callers attach `refCallback` to the scrolling container. `verticalScrollRef` exposes the
 * same node for downstream usage (ScrollArrows, react-list's `scrollParentGetter`).
 */
export function useTmTableHeight() {
  const theme = useTheme();
  const verticalScrollRef = useRef<HTMLDivElement | null>(null);
  const [tableHeight, setTableHeight] = useState(600);

  const onResize = useCallback(() => {
    const position = verticalScrollRef.current?.getBoundingClientRect();
    if (position) {
      const bottomSpacing = parseInt(theme.spacing(2), 10);
      setTableHeight(window.innerHeight - position.top - bottomSpacing);
    }
  }, [theme]);

  const refCallback = useCallback(
    (node: HTMLDivElement | null) => {
      verticalScrollRef.current = node;
      onResize();
    },
    [onResize]
  );

  useEffect(() => {
    onResize();
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, [onResize]);

  useResizeObserver({ ref: verticalScrollRef, onResize });

  return { tableHeight, verticalScrollRef, refCallback };
}
