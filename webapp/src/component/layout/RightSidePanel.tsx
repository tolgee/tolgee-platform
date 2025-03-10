import { Box, ClickAwayListener, styled } from '@mui/material';
import { useRef } from 'react';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
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
  children?: React.ReactNode;
  floating?: boolean;
  open?: boolean;
  onClose?: () => void;
  width: number;
};

export const RightSidePanel = ({
  children,
  floating,
  open = true,
  onClose,
  width,
}: Props) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const topBannerHeight = useGlobalContext((c) => c.layout.topBannerHeight);
  const topBarHeight = useGlobalContext((c) => c.layout.topBarHeight);

  const [openedDebounced] = useDebounce(open, 100);

  const handleClickAway = () => {
    if (openedDebounced && floating) {
      onClose?.();
    }
  };

  if (!width) {
    return null;
  }

  return (
    <ClickAwayListener onClickAway={handleClickAway}>
      <StyledPanel
        sx={{
          top: topBannerHeight,
          transform: `translate(0px, ${topBarHeight}px)`,
          paddingBottom: topBarHeight + 'px',
          width,
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
