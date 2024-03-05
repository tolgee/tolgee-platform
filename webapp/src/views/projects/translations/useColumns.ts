import { useEffect, useMemo, useRef, useState } from 'react';
import { resizeColumn, useResize } from './useResize';

type PassedRefType = React.RefObject<HTMLElement | undefined>;

type Props = {
  tableRef: PassedRefType;
  initialRatios: number[];
  minSize?: number;
  deps?: React.DependencyList;
};

export const useColumns = ({
  tableRef,
  initialRatios,
  minSize,
  deps = [],
}: Props) => {
  const [columnSizes, setColumnSizes] = useState<number[]>(initialRatios);
  const resizersCallbacksRef = useRef<(() => void)[]>([]);

  const { width } = useResize(tableRef, deps);

  const columnSizesPercent = useMemo(() => {
    const columnsSum = columnSizes?.reduce((a, b) => a + b, 0) || 0;

    if (minSize) {
      return columnSizes?.map((size) => size + 'px');
    }

    return columnSizes?.map((size) => (size / columnsSum) * 100 + '%');
  }, [columnSizes, width]);

  function calcualteRealSize(prevSizes: number[]) {
    const previousWidth = prevSizes?.reduce((a, b) => a + b, 0) || 1;
    const newSizes = prevSizes?.map((w) => {
      const newSize = (w / previousWidth) * (width || 1);
      if (minSize && newSize < minSize) {
        return minSize;
      }
      return newSize;
    });
    return newSizes;
  }

  useEffect(() => {
    setColumnSizes(calcualteRealSize(initialRatios));
  }, [initialRatios.length]);

  useEffect(() => {
    setColumnSizes(calcualteRealSize(columnSizes));
  }, [width, minSize]);

  const actions = {
    startResize(index: number) {
      resizersCallbacksRef.current[index]?.();
    },
    resizeColumn(index: number, size: number) {
      if (columnSizes) {
        setColumnSizes(
          resizeColumn({
            allSizes: columnSizes,
            index,
            newSize: size,
            minSize,
            originalRatios: initialRatios,
          })
        );
      }
    },
    addResizer(index: number, callback: () => void) {
      resizersCallbacksRef.current[index] = callback;
    },
  };

  const context = {
    totalWidth: width,
    columnSizes: columnSizes || [],
    columnSizesPercent: columnSizesPercent || [],
  };

  return { ...context, ...actions };
};
