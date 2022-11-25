import { useEffect, useMemo, useRef, useState } from 'react';
import { createProviderNew } from 'tg.fixtures/createProviderNew';
import { useResize, resizeColumn } from '../useResize';

export const [ColumnsContext, useColumnsActions, useColumnsContext] =
  createProviderNew(() => {
    const [initialColSizes, setInitialColSizes] = useState<number[]>();

    const [columnSizes, setColumnSizes] = useState<number[]>();

    const [tableEl, setTableEl] = useState<HTMLDivElement | null>();
    const resizersCallbacksRef = useRef<(() => void)[]>([]);

    const { width } = useResize(tableEl, initialColSizes);

    const columnSizesPercent = useMemo(() => {
      const columnsSum = columnSizes?.reduce((a, b) => a + b, 0) || 0;
      return columnSizes?.map((size) => (size / columnsSum) * 100 + '%');
    }, [columnSizes]);

    useEffect(() => {
      const prevSizes =
        columnSizes?.length === initialColSizes?.length
          ? columnSizes
          : initialColSizes;
      const previousWidth = prevSizes?.reduce((a, b) => a + b, 0) || 1;
      const newSizes = prevSizes?.map(
        (w) => (w / previousWidth) * (width || 1)
      );
      setColumnSizes(newSizes);
    }, [width, initialColSizes]);

    const actions = {
      startResize(index: number) {
        resizersCallbacksRef.current[index]?.();
      },
      resizeColumn(index: number, size: number) {
        if (columnSizes) {
          setColumnSizes(resizeColumn(columnSizes, index, size));
        }
      },

      resetColumns(initialSizes: number[], element: HTMLDivElement | null) {
        setInitialColSizes(initialSizes);
        setTableEl(element);
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
