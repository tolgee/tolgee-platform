import { useState, useEffect } from 'react';

export const useResize = (myRef, dependency) => {
  const [width, setWidth] = useState(undefined);

  const handleResize = () => {
    const newWidth = myRef.current?.offsetWidth;
    if (newWidth && width !== newWidth) {
      setWidth(newWidth);
    }
  };

  useEffect(() => {
    handleResize();
  }, [dependency]);

  useEffect(() => {
    window.addEventListener('resize', handleResize);
    const interval = setInterval(handleResize, 1000);
    return () => {
      clearInterval(interval);
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  return { width: width || 0 };
};

export const resizeColumn = (
  allSizes: number[],
  index: number,
  newSize: number,
  minSizeMult = 0.5
) => {
  const oldColumnSize = allSizes[index];
  let newColumnSize = newSize;
  const totalSize = allSizes.reduce((a, b) => a + b, 0);
  const minSize = (totalSize / allSizes.length) * minSizeMult;
  const columnsAfter = allSizes.slice(index + 1);
  const marginsAfter = columnsAfter.map((w) => w - minSize);
  const maxIncrease = marginsAfter.reduce((a, b) => a + b, 0);
  if (newColumnSize < minSize) {
    newColumnSize = minSize;
  } else if (newColumnSize - oldColumnSize > maxIncrease) {
    newColumnSize = oldColumnSize + maxIncrease;
  }
  const columnsBefore = allSizes.slice(0, index);

  const newAfterSizes = marginsAfter.map((w) => {
    const portion = maxIncrease ? w / maxIncrease : 1 / marginsAfter.length;
    return minSize + (w - portion * (newColumnSize - oldColumnSize));
  });
  return [...columnsBefore, newColumnSize, ...newAfterSizes];
};
