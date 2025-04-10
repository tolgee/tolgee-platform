import { RefObject, useState } from 'react';
import { useIsMounted, useResizeObserver } from 'usehooks-ts';

/**
 * usehook-ts resizeObserver is causing "ResizeObserver loop completed with undelivered notifications"
 * in cypress tests and in safari
 *
 * This is optimized version, which only observes width and tries to minimize the error occurrence
 */

type OriginalOptions = Parameters<typeof useResizeObserver>[0];
type Props<T extends HTMLElement = HTMLElement> = {
  ref: RefObject<T>;
  box?: OriginalOptions['box'];
};

export const useWidthObserver = ({ ref, box }: Props): number | undefined => {
  const [elementWidth, setElementWidth] = useState<number | undefined>(
    undefined
  );

  const mounted = useIsMounted();

  const size = useResizeObserver({
    ref,
    box,
    onResize(size) {
      requestAnimationFrame(() => {
        if (mounted()) {
          setElementWidth(size.width);
        }
      });
    },
  });

  return elementWidth ?? size.width;
};
