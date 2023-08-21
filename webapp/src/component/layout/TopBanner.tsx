import { styled } from '@mui/material';
import { useEffect, useRef } from 'react';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';

const StyledContainer = styled('div')`
  position: fixed;
  top: 0px;
  left: 0px;
  right: 0px;
  display: flex;
  justify-content: center;
  height: 30px;
  align-items: center;
  background: ${({ theme }) => theme.palette.topBanner.background};
  z-index: ${({ theme }) => theme.zIndex.drawer + 2};
`;

export function TopBanner() {
  const bannerContent = useGlobalContext((c) => c.topBanner?.content);
  const { setTopBannerHeight } = useGlobalActions();
  const bannerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setTopBannerHeight(bannerRef.current?.offsetHeight || 0);
  }, [bannerContent]);

  if (!bannerContent) {
    return null;
  }

  return <StyledContainer ref={bannerRef}>{bannerContent}</StyledContainer>;
}
