import { RefObject, useState } from 'react';
import {
  useIsMounted,
  useResizeObserver as useOriginalResizeObserver,
} from 'usehooks-ts';

/**
 * usehook-ts resizeObserver is causing "ResizeObserver loop completed with undelivered notifications"
 * in cypress tests and in safari
 *
 * Delaying the event reaction, seems to solve the issue
 */

type OriginalOptions = Parameters<typeof useOriginalResizeObserver>[0];
type Size = {
  /** The width of the observed element. */
  width: number | undefined;
  /** The height of the observed element. */
  height: number | undefined;
};
type Props<T extends HTMLElement = HTMLElement> = {
  ref: RefObject<T>;
  box?: OriginalOptions['box'];
  onResize?: OriginalOptions['onResize'];
};

export const useResizeObserver = ({ ref, box, onResize }: Props): Size => {
  const [bodySize, setBodySize] = useState<Size | undefined>(undefined);

  const mounted = useIsMounted();

  const size = useOriginalResizeObserver({
    ref,
    box,
    onResize(size) {
      setTimeout(() => {
        if (mounted()) {
          if (onResize) {
            onResize(size);
          } else {
            setBodySize(size as Size);
          }
        }
      }, 0);
    },
  });

  return bodySize ?? size;
};
