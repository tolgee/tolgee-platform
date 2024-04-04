import { Box, ClickAwayListener, styled } from '@mui/material';
import clsx from 'clsx';
import { useEffect, useRef } from 'react';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { useDebounce } from 'use-debounce';
import { useResizeObserver } from 'usehooks-ts';

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
  onClose: () => void;
  floating: boolean;
  open: boolean;
};

export const RightSidePanel = ({
  children,
  floating,
  onClose,
  open,
}: Props) => {
  const { setRightPanelWidth } = useGlobalActions();

  const containerRef = useRef<HTMLDivElement>(null);
  const topBannerHeight = useGlobalContext((c) => c.layout.topBannerHeight);
  const topBarHeight = useGlobalContext((c) => c.layout.topBarHeight);

  useResizeObserver({
    ref: containerRef,
    onResize({ width = 0 }) {
      if (!floating && open) {
        setRightPanelWidth(width);
      } else {
        setRightPanelWidth(0);
      }
    },
  });
  useEffect(() => {
    if (!floating && open) {
      setRightPanelWidth(containerRef.current?.offsetWidth || 0);
    } else {
      setRightPanelWidth(0);
    }
    return () => setRightPanelWidth(0);
  }, [floating, open]);

  const [openedDebounced] = useDebounce(open, 100);

  const handleClickAway =
    openedDebounced && floating ? () => onClose() : () => {};

  return (
    <ClickAwayListener onClickAway={handleClickAway}>
      <StyledPanel
        sx={{
          top: topBannerHeight,
          transform: `translate(0px, ${topBarHeight}px)`,
          paddingBottom: topBarHeight + 'px',
        }}
        style={{
          right: open ? '0%' : '-105%',
        }}
        className={clsx({ floating })}
        ref={containerRef}
      >
        {children}
      </StyledPanel>
    </ClickAwayListener>
  );
};
