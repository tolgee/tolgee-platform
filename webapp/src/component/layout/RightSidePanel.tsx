import { Box, ClickAwayListener, styled } from '@mui/material';
import { useRef } from 'react';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { useDebounce } from 'use-debounce';

const StyledPanel = styled(Box)`
  position: fixed;
  right: 0px;
  bottom: 0px;
  width: 400px;
  max-width: 100vw;
  transition: transform 0.2s ease-in-out, right 0.5s ease;
  top: 0;
  box-shadow: 0px 4px 10px 0px rgba(0, 0, 0, 0.1);
  background: ${({ theme }) => theme.palette.background.default};
  box-sizing: border-box;
  z-index: ${({ theme }) => theme.zIndex.drawer + 2};
  @media (max-width: ${1200}px) {
    box-shadow: 0px 4px 10px 0px rgba(0, 0, 0, 0.5);
    right: -105%;
  }
`;

type Props = {
  children: React.ReactNode;
};

export const RightSidePanel = ({ children }: Props) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const topBannerHeight = useGlobalContext((c) => c.layout.topBannerHeight);
  const topBarHeight = useGlobalContext((c) => c.layout.topBarHeight);
  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);
  const rightPanelFloating = useGlobalContext(
    (c) => c.layout.rightPanelFloating
  );
  const shouldFloat = useGlobalContext((c) => c.layout.rightPanelShouldFloat);
  const { setQuickStartFloatingOpen } = useGlobalActions();

  const open = rightPanelWidth || rightPanelFloating;

  const [openedDebounced] = useDebounce(open, 100);

  const handleClickAway =
    openedDebounced && shouldFloat
      ? () => setQuickStartFloatingOpen(false)
      : () => {};

  return (
    <ClickAwayListener onClickAway={handleClickAway}>
      <StyledPanel
        sx={{
          top: topBannerHeight,
          transform: `translate(0px, ${topBarHeight}px)`,
          paddingBottom: topBarHeight + 'px',
          width: rightPanelWidth || 400,
        }}
        style={{
          right: open ? '0%' : '-105%',
        }}
        ref={containerRef}
      >
        {children}
      </StyledPanel>
    </ClickAwayListener>
  );
};
