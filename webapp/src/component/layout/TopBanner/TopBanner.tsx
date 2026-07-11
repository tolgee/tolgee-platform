import { useEffect, useRef } from 'react';
import { styled } from '@mui/material';
import clsx from 'clsx';
import { Mail01, XClose } from '@untitled-ui/icons-react';

import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { useAnnouncement } from './useAnnouncement';
import { useIsEmailVerified } from 'tg.globalContext/helpers';
import { useResizeObserver } from 'usehooks-ts';
import { tokenService } from 'tg.service/TokenService';
import { PendingInvitationBanner } from './PendingInvitationBanner';
import { useTranslate } from '@tolgee/react';
import { Announcement } from './Announcement';

const StyledContainer = styled('div')`
  position: fixed;
  top: 0px;
  left: 0px;
  right: 0px;
  display: grid;
  grid-template-columns: 50px 1fr 50px;
  width: 100%;
  z-index: ${({ theme }) => theme.zIndex.drawer + 2};
  background: ${(props) => props.theme.palette.topBanner.background};

  &.emailNotVerified {
    color: ${({ theme }) =>
      theme.palette.tokens._components.noticeBar.importantColor};
    background-color: ${({ theme }) =>
      theme.palette.tokens._components.noticeBar.importantFill};
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
  const { t } = useTranslate();
  const bannerType = useGlobalContext((c) => c.initialData.announcement?.type);
  const pendingInvitationCode = useGlobalContext((c) => c.auth.invitationCode);
  const { setTopBannerHeight, dismissAnnouncement } = useGlobalActions();
  const bannerRef = useRef<HTMLDivElement>(null);
  const isAuthenticated = tokenService.getToken() !== undefined;

  const getAnnouncement = useAnnouncement();
  const isEmailVerified = useIsEmailVerified();

  const showEmailVerificationBanner = !isEmailVerified && isAuthenticated;

  const announcement = bannerType && getAnnouncement(bannerType);
  const showCloseButton =
    !showEmailVerificationBanner && !pendingInvitationCode;

  useResizeObserver({
    ref: bannerRef,
    onResize({ height = 0 }) {
      setTopBannerHeight(height);
    },
  });

  useEffect(() => {
    const height = bannerRef.current?.offsetHeight;
    setTopBannerHeight(height ?? 0);
  }, [announcement, isEmailVerified, pendingInvitationCode]);

  if (!announcement && !pendingInvitationCode && !showEmailVerificationBanner) {
    return null;
  }

  return (
    <StyledContainer
      ref={bannerRef}
      data-cy="top-banner"
      className={clsx({ emailNotVerified: showEmailVerificationBanner })}
    >
      <div />
      <StyledContent data-cy="top-banner-content">
        {showEmailVerificationBanner ? (
          <Announcement
            content={null}
            title={t('verify_email_account_not_verified_title')}
            icon={<Mail01 />}
          />
        ) : pendingInvitationCode ? (
          <PendingInvitationBanner code={pendingInvitationCode} />
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
