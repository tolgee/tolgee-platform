import { Close } from '@mui/icons-material';
import {
  Box,
  ClickAwayListener,
  IconButton,
  styled,
  useMediaQuery,
} from '@mui/material';
import clsx from 'clsx';
import { useEffect, useRef } from 'react';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { useDebounce } from 'use-debounce/lib';

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
  @media (max-width: ${1200}px) {
    box-shadow: 0px 4px 10px 0px rgba(0, 0, 0, 0.5);
    z-index: ${({ theme }) => theme.zIndex.drawer};
    right: -105%;
  }
`;

type Props = {
  children: React.ReactNode;
};

export const RightSidePanel = ({ children }: Props) => {
  const floating = useMediaQuery(`@media (max-width: ${1200}px)`);
  const { setRightPanelWidth, setRightPanelOpen } = useGlobalActions();
  const containerRef = useRef<HTMLDivElement>(null);
  const topBannerHeight = useGlobalContext((c) => c.topBannerHeight);
  const topBarHeight = useGlobalContext((c) => c.topBarHeight);
  const rightPanelOpen = useGlobalContext((c) => c.rightPanelOpen);

  useEffect(() => {
    if (!floating) {
      setRightPanelWidth(containerRef.current?.offsetWidth || 0);
    }
    return () => setRightPanelWidth(0);
  }, [floating]);

  useEffect(() => {
    function handler() {
      setRightPanelWidth(containerRef.current?.offsetWidth || 0);
    }
    if (!floating) {
      window.addEventListener('resize', handler);
      return () => window.removeEventListener('resize', handler);
    }
  }, [floating]);

  const [openedDebounced] = useDebounce(rightPanelOpen, 100);

  const handleClickAway = openedDebounced
    ? () => setRightPanelOpen(false)
    : () => {};

  return (
    <ClickAwayListener onClickAway={handleClickAway}>
      <StyledPanel
        sx={{
          top: topBannerHeight,
          transform: `translate(0px, ${topBarHeight}px)`,
          paddingBottom: topBarHeight + 'px',
        }}
        style={{
          right: rightPanelOpen ? '0%' : undefined,
        }}
        className={clsx({ floating })}
        ref={containerRef}
      >
        {floating && (
          <IconButton
            sx={{ position: 'absolute', right: 10, top: 10 }}
            onClick={() => setRightPanelOpen(false)}
          >
            <Close />
          </IconButton>
        )}
        {children}
      </StyledPanel>
    </ClickAwayListener>
  );
};
