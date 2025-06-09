const widthMap = {
  wide: 1200,
  normal: 900,
  narrow: 600,
  max: undefined,
};

export type BaseViewWidth = keyof typeof widthMap | number | undefined;

export function getBaseViewWidth(width: BaseViewWidth) {
  return typeof width === 'string' ? widthMap[width] : width;
}
