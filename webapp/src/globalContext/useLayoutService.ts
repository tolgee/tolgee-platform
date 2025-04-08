import { useState } from 'react';
import { useWindowSize } from 'usehooks-ts';
import type { useQuickStartGuideService } from './useQuickStartGuideService';

export const TOP_BAR_HEIGHT = 52;

export type RightPanelContent = 'quick_start';

type Props = {
  quickStart: ReturnType<typeof useQuickStartGuideService>;
};

export const useLayoutService = ({ quickStart }: Props) => {
  const [topBannerHeight, setTopBannerHeight] = useState(0);
  const [topSubBannerHeight, setTopSubBannerHeight] = useState(0);
  const [topBarHidden, setTopBarHidden] = useState(false);
  const viewPortSize = useWindowSize();
  const [rightPanelFloatingForced, setRightPanelFloatingForced] =
    useState(false);

  const bodyWidth = viewPortSize.width;

  const rightPanelShouldFloat = bodyWidth < 1200 || rightPanelFloatingForced;

  const rightPanelFloating =
    quickStart.state.floatingOpen &&
    quickStart.state.enabled &&
    rightPanelShouldFloat;

  const rightPanelWidth =
    !rightPanelShouldFloat && quickStart.state.open && quickStart.state.enabled
      ? Math.min(400, bodyWidth)
      : 0;

  const state = {
    topBannerHeight,
    topSubBannerHeight,
    bodyWidth,
    rightPanelWidth,
    rightPanelFloating,
    rightPanelShouldFloat,
    topBarHeight: topBarHidden ? 0 : TOP_BAR_HEIGHT,
  };

  const actions = {
    setTopBannerHeight,
    setTopSubBannerHeight,
    setTopBarHidden,
    setRightPanelFloatingForced,
  };

  return { state, actions };
};
