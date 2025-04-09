import { useEffect, useRef, useState } from 'react';
import { useResizeObserver } from 'usehooks-ts';

export const useBodySize = () => {
  const [bodySize, setBodySize] = useState({
    width: document.body.offsetWidth,
    height: document.body.offsetHeight,
  });

  const mounted = useRef(true);

  useEffect(() => {
    return () => {
      mounted.current = false;
    };
  }, []);

  useResizeObserver({
    ref: { current: document.body },
    onResize(size) {
      if (mounted.current) {
        setBodySize(size as any);
      }
    },
  });

  return bodySize;
};
