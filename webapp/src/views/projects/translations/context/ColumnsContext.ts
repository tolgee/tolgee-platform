import { useEffect, useMemo, useRef, useState } from 'react';
import { createProviderNew } from 'tg.fixtures/createProviderNew';
import { useResize, resizeColumn } from '../useResize';

type PassedRefType = React.RefObject<HTMLElement | undefined>;

export const [ColumnsContext, useColumnsActions, useColumnsContext] =
  createProviderNew(() => {
    const [columnSizes, setColumnSizes] = useState<number[]>();

    const [tableRef, setTableRef] = useState<PassedRefType>({
      current: undefined,
    });
    const resizersCallbacksRef = useRef<(() => void)[]>([]);

    const { width } = useResize(tableRef, columnSizes);

    const columnSizesPercent = useMemo(() => {
      const columnsSum = columnSizes?.reduce((a, b) => a + b, 0) || 0;
      return columnSizes?.map((size) => (size / columnsSum) * 100 + '%');
    }, [columnSizes]);

    function calcualteRealSize(prevSizes: number[] | undefined) {
      const previousWidth = prevSizes?.reduce((a, b) => a + b, 0) || 1;
      const newSizes = prevSizes?.map(
        (w) => (w / previousWidth) * (width || 1)
      );
      return newSizes;
    }

    useEffect(() => {
      setColumnSizes(calcualteRealSize(columnSizes));
    }, [width]);

    const actions = {
      startResize(index: number) {
        resizersCallbacksRef.current[index]?.();
      },
      resizeColumn(index: number, size: number) {
        if (columnSizes) {
          setColumnSizes(resizeColumn(columnSizes, index, size));
        }
      },

      resetColumns(sizeRatio: number[], elementRef: PassedRefType) {
        setColumnSizes(calcualteRealSize(sizeRatio));
        setTableRef(elementRef);
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

    return [context, actions];
  });
