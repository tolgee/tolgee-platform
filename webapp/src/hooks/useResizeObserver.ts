import { RefObject, useState } from 'react';
import {
  useIsMounted,
  useResizeObserver as useOriginalResizeObserver,
} from 'usehooks-ts';

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

export const useResizeObserver = ({ ref, box }: Props): Size => {
  const [bodySize, setBodySize] = useState<Size | undefined>(undefined);

  const mounted = useIsMounted();

  const size = useOriginalResizeObserver({
    ref,
    box,
    onResize(size) {
      requestAnimationFrame(() => {
        if (mounted()) {
          setBodySize(size as Size);
        }
      });
    },
  });

  return bodySize ?? size;
};
