import { Component, JSX } from 'react';

type ItemRenderer = (index: number, key: number | string) => JSX.Element;
type ItemsRenderer = (items: JSX.Element[], ref: string) => JSX.Element;
type ItemSizeEstimator = (
  index: number,
  cache: Record<number, number>
) => number;
type ItemSizeGetter = (index: number) => number;
type ScrollParentGetter = () => JSX.Element;

interface ReactListProps {
  children?: React.ReactNode;
  ref?: React.LegacyRef<ReactList> | undefined;
  axis?: 'x' | 'y' | undefined;
  initialIndex?: number | undefined;
  itemRenderer?: ItemRenderer | undefined;
  itemSizeEstimator?: ItemSizeEstimator | undefined;
  itemSizeGetter?: ItemSizeGetter | undefined;
  itemsRenderer?: ItemsRenderer | undefined;
  length?: number | undefined;
  minSize?: number | undefined;
  pageSize?: number | undefined;
  scrollParentGetter?: ScrollParentGetter | undefined;
  threshold?: number | undefined;
  type?: string | undefined;
  useStaticSize?: boolean | undefined;
  useTranslate3d?: boolean | undefined;
  /**
   * Extra pixels added to the inner container's size so the layout stays stable when an
   * item expands inline (e.g. a cell entering edit mode). Set to a non-zero value only when
   * such expansion is currently possible — otherwise it shows up as phantom scroll space at
   * the bottom of the list. Default: 0.
   */
  expansionReserve?: number | undefined;
}

export declare class ReactList extends Component<ReactListProps> {
  scrollTo(index: number): void;
  scrollAround(index: number): void;
  getVisibleRange(): number[];
}
