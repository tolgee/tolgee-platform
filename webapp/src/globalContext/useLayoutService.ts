import { useState } from 'react';

export const TOP_BAR_HEIGHT = 52;

export const useLayoutService = () => {
  const [topBannerHeight, setTopBannerHeight] = useState(0);
  const [topSubBannerHeight, setTopSubBannerHeight] = useState(0);
  const [rightPanelWidth, setRightPanelWidth] = useState(0);
  const [topBarHidden, setTopBarHidden] = useState(false);

  const state = {
    topBannerHeight,
    topSubBannerHeight,
    rightPanelWidth,
    topBarHeight: topBarHidden ? 0 : TOP_BAR_HEIGHT,
  };

  const actions = {
    setTopBannerHeight,
    setTopSubBannerHeight,
    setRightPanelWidth,
    setTopBarHidden,
  };

  return { state, actions };
};
