import {styled} from '@mui/material';
import {useEffect, useRef} from 'react';
import {useGlobalActions, useGlobalContext,} from 'tg.globalContext/GlobalContext';
import {useAnnouncement} from './useAnnouncement';
import {useIsEmailVerified} from 'tg.globalContext/helpers';
import {Close} from '@mui/icons-material';
import {useResizeObserver} from 'usehooks-ts';

const StyledContainer = styled('div')`
  position: fixed;
  top: 0px;
  left: 0px;
  right: 0px;
  display: grid;
  grid-template-columns: 50px 1fr 50px;
  width: 100%;
  background: ${({ theme}) => useIsEmailVerified() ? theme.palette.topBanner.background : theme.palette.emailNotVerifiedBanner.background};
  z-index: ${({ theme }) => theme.zIndex.drawer + 2};
  color: ${({ theme }) => useIsEmailVerified() ? theme.palette.topBanner.mainText : theme.palette.emailNotVerifiedBanner.mainText};
  font-size: 15px;
  font-weight: 700;
  @container (max-width: 899px) {
    grid-template-columns: 0px 1fr 50px;
  }
`;

const StyledContent = styled('div')`
  padding: 8px 15px;
  display: flex;
  justify-content: center;
`;

const StyledCloseButton = styled('div')`
  display: flex;
  align-items: center;
  align-self: start;
  justify-self: center;
  padding: 2px;
  cursor: pointer;
  margin: 6px 18px;
`;

export function TopBanner() {

  const bannerType = useGlobalContext((c) => c.initialData.announcement?.type);
  const { setTopBannerHeight, dismissAnnouncement } = useGlobalActions();
  const bannerRef = useRef<HTMLDivElement>(null);

  const getAnnouncement = useAnnouncement();
  const announcement = bannerType && getAnnouncement(bannerType);
  const showCloseButton = useIsEmailVerified()

  useResizeObserver({
    ref: bannerRef,
    onResize({ height = 0 }) {
      setTopBannerHeight(height);
    },
  });

  useEffect(() => {
    const height = bannerRef.current?.offsetHeight;
    setTopBannerHeight(height ?? 0);
  }, [announcement]);

  if (!announcement) {
    return null;
  }

  return (
    <StyledContainer ref={bannerRef} data-cy="top-banner">
      <div />
      <StyledContent data-cy="top-banner-content">{announcement}</StyledContent>
      {showCloseButton && (<StyledCloseButton
        role="button"
        tabIndex={0}
        onClick={() => dismissAnnouncement()}
        data-cy="top-banner-dismiss-button"
      >
        <Close />
      </StyledCloseButton>
      )}
    </StyledContainer>
  );
}
