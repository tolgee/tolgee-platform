import { styled } from '@mui/material';
import { useEffect, useRef } from 'react';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';

import { Close } from '@mui/icons-material';

const StyledContainer = styled('div')`
  position: fixed;
  top: 0px;
  left: 0px;
  right: 0px;
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  width: 100%;
  background: ${({ theme }) => theme.palette.topBanner.background};
  z-index: ${({ theme }) => theme.zIndex.drawer + 2};
`;

const StyledContent = styled('div')`
  text-align: center;
  padding: 3px 15px;
`;

const StyledCloseButton = styled('div')`
  display: flex;
  align-items: center;
  align-self: start;
  justify-self: center;
  padding: 2px;
  cursor: pointer;
  margin: 2px 4px;
`;

export function TopBanner() {
  const bannerContent = useGlobalContext((c) => c.topBanner?.content);
  const { setTopBannerHeight, dismissTopBanner } = useGlobalActions();
  const bannerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setTopBannerHeight(bannerRef.current?.offsetHeight || 0);
  }, [bannerContent]);

  useEffect(() => {
    function handler() {
      setTopBannerHeight(bannerRef.current?.offsetHeight || 0);
    }
    window.addEventListener('resize', handler);
    return () => window.removeEventListener('resize', handler);
  }, []);

  if (!bannerContent) {
    return null;
  }

  return (
    <StyledContainer ref={bannerRef}>
      <div />
      <StyledContent>{bannerContent}</StyledContent>
      <StyledCloseButton role="button" tabIndex={0} onClick={dismissTopBanner}>
        <Close />
      </StyledCloseButton>
    </StyledContainer>
  );
}
