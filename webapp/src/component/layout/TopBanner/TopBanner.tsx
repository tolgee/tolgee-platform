import { styled, useTheme } from '@mui/material';
import { useEffect, useRef } from 'react';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { useAnnouncement } from './useAnnouncement';
import { useIsEmailVerified } from 'tg.globalContext/helpers';
import { XClose } from '@untitled-ui/icons-react';
import { useResizeObserver } from 'usehooks-ts';
import { Announcement } from 'tg.component/layout/TopBanner/Announcement';
import { useTranslate } from '@tolgee/react';
import { tokenService } from 'tg.service/TokenService';

const StyledContainer = styled('div')`
  position: fixed;
  top: 0px;
  left: 0px;
  right: 0px;
  display: grid;
  grid-template-columns: 50px 1fr 50px;
  width: 100%;
  z-index: ${({ theme }) => theme.zIndex.drawer + 2};
  &.email-verified {
    color: ${(props) => props.theme.palette.topBanner.mainText};
    background: ${(props) => props.theme.palette.topBanner.background};
  }

  &.email-not-verified {
    color: ${(props) =>
      props.theme.palette.tokens._components.noticeBar.importantLink};
    background: ${(props) =>
      props.theme.palette.tokens._components.noticeBar.importantFill};
  }
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
  const isAuthenticated = tokenService.getToken() !== undefined;

  const getAnnouncement = useAnnouncement();
  const isEmailVerified = useIsEmailVerified();
  const announcement = bannerType && getAnnouncement(bannerType);
  const showCloseButton = isEmailVerified;
  const containerClassName = isEmailVerified
    ? 'email-verified'
    : 'email-not-verified';
  const theme = useTheme();
  const mailImage =
    theme.palette.mode === 'dark'
      ? '/images/mailDark.svg'
      : '/images/mailLight.svg';
  const { t } = useTranslate();

  useResizeObserver({
    ref: bannerRef,
    onResize({ height = 0 }) {
      setTopBannerHeight(height);
    },
  });

  useEffect(() => {
    const height = bannerRef.current?.offsetHeight;
    setTopBannerHeight(height ?? 0);
  }, [announcement, isEmailVerified]);

  if (!announcement && (isEmailVerified || !isAuthenticated)) {
    return null;
  }

  return (
    <StyledContainer
      ref={bannerRef}
      data-cy="top-banner"
      className={containerClassName}
    >
      <div />
      <StyledContent data-cy="top-banner-content">
        {!isEmailVerified ? (
          <Announcement
            content={null}
            title={t('verify_email_account_not_verified_title')}
            icon={<img src={mailImage} alt="Mail Icon" />}
          />
        ) : (
          announcement
        )}
      </StyledContent>
      {showCloseButton && (
        <StyledCloseButton
          role="button"
          tabIndex={0}
          onClick={() => dismissAnnouncement()}
          data-cy="top-banner-dismiss-button"
        >
          <XClose />
        </StyledCloseButton>
      )}
    </StyledContainer>
  );
}
