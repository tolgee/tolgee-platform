const minSizeMult = 0.5;

type Props = {
  allSizes: number[];
  index: number;
  newSize: number;
  originalRatios: number[];
  minSize?: number;
};

export const resizeColumn = ({
  allSizes,
  index,
  newSize,
  originalRatios,
  minSize,
}: Props) => {
  const oldColumnSize = allSizes[index];
  let newColumnSize = newSize;
  const totalSize = allSizes.reduce((a, b) => a + b, 0);
  let minSizeCalculated: number;
  const originalSum = originalRatios.reduce((prev, curr) => prev + curr, 0);
  const originalSizes = originalRatios.map(
    (ratio) => (totalSize / originalSum) * ratio
  );
  if (minSize === undefined) {
    minSizeCalculated = originalSizes[index] * minSizeMult;
  } else {
    minSizeCalculated = minSize;
  }
  const margins = allSizes.map(
    (size, i) => size - originalSizes[i] * minSizeMult
  );
  const originalRatiosAfter = originalRatios.slice(index + 1);
  const columnsAfter = allSizes.slice(index + 1);
  const marginsAfter = margins.slice(index + 1);
  const originalRatiosSum = originalRatiosAfter.reduce(
    (prev, curr) => prev + curr
  );

  const maxIncrease = marginsAfter.reduce((a, b) => a + b, 0);

  if (newColumnSize < minSizeCalculated) {
    newColumnSize = minSizeCalculated;
  } else if (newColumnSize - oldColumnSize > maxIncrease) {
    newColumnSize = oldColumnSize + maxIncrease;
  }
  const columnsBefore = allSizes.slice(0, index);
  const difference = newColumnSize - oldColumnSize;

  let newAfterSizes: number[];
  if (!minSize) {
    newAfterSizes = columnsAfter.map((size, i) => {
      const portion = originalRatiosAfter[i] / originalRatiosSum;
      return size - portion * difference;
    });
  } else {
    newAfterSizes = columnsAfter;
  }

  const result = [...columnsBefore, newColumnSize, ...newAfterSizes];
  return result;
};
