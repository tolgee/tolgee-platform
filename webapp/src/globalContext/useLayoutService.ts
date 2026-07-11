import { useState } from 'react';
import type { useQuickStartGuideService } from './useQuickStartGuideService';
import { useWidthObserver } from 'tg.hooks/useWidthObserver';

export const TOP_BAR_HEIGHT = 52; // the same value as theme.mixins.toolbar.minHeight

type Props = {
  quickStart: ReturnType<typeof useQuickStartGuideService>;
  aiPlaygroundEnabled: boolean;
};

export const useLayoutService = ({
  quickStart,
  aiPlaygroundEnabled,
}: Props) => {
  const [topBannerHeight, setTopBannerHeight] = useState(0);
  const [topSubBannerHeight, setTopSubBannerHeight] = useState(0);
  const [topBarHidden, setTopBarHidden] = useState(false);
  const bodyWidth =
    useWidthObserver({
      ref: {
        current: typeof document !== 'undefined' ? document.body : null,
      },
    }) ?? 0;

  const rightPanelHidden =
    bodyWidth < 1200 ||
    ((!quickStart.state.enabled ||
      !quickStart.state.open ||
      quickStart.state.floatingForced) &&
      !aiPlaygroundEnabled);

  const quickStartFloating =
    quickStart.state.enabled &&
    (quickStart.state.floatingForced || aiPlaygroundEnabled);

  const desiredWidth = aiPlaygroundEnabled ? Math.max(400, bodyWidth / 3) : 400;

  const rightPanelWidth = !rightPanelHidden
    ? Math.min(desiredWidth, bodyWidth)
    : 0;

  const state = {
    topBannerHeight,
    topSubBannerHeight,
    bodyWidth,
    rightPanelWidth,
    quickStartFloating,
    topBarHeight: topBarHidden ? 0 : TOP_BAR_HEIGHT,
  };

  const actions = {
    setTopBannerHeight,
    setTopSubBannerHeight,
    setTopBarHidden,
  };

  return { state, actions };
};
