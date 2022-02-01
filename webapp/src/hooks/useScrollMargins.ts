import { useBottomPanel } from 'tg.component/bottomPanel/BottomPanelContext';

type Props = {
  top?: number;
  bottom?: number;
};

export const useScrollMargins = (props?: Props) => {
  const { top = 50, bottom = 80 } = props || {};
  const { height: bottomPanelHeight } = useBottomPanel();
  return {
    scrollMarginTop: top,
    scrollMarginBottom: bottom + bottomPanelHeight,
  };
};
