import { Link as MuiLink, styled, Typography, useTheme } from '@mui/material';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { T, useTranslate } from '@tolgee/react';
import { useEmailAwaitingVerification } from 'tg.globalContext/helpers';
import { LINKS } from 'tg.constants/links';
import { Usage } from 'tg.component/billing/Usage';
import { StyledWrapper } from 'tg.component/searchSelect/SearchStyled';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { BaseView } from 'tg.component/layout/BaseView';
import { Redirect } from 'react-router-dom';
import { useState } from 'react';
import { useTimerCountdown } from 'tg.fixtures/useTimerCountdown';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-top: 5vh;
`;

const StyledHeader = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.primary}
  font-size: 24px;
  font-style: normal;
  font-weight: 500;
  text-align: center;
  margin-bottom: 20px;
`;

const StyledDescription = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.primary}
  margin-bottom: 40px;
  text-align: center;
  width: 80%;
  margin-left: auto;
  margin-right: auto;
  max-width: 60%;
`;

export const StyledLink = styled(MuiLink)`
  cursor: pointer;
  font-weight: 400;

  &.disabled {
    color: ${({ theme }) => theme.palette.emphasis[400]};
    pointer-events: none;
  }
`;

const StyledImg = styled('img')`
  margin-top: 20px;
  margin-bottom: 30px;
`;

const BoldSpan = styled('span')`
  font-weight: 500;
`;

export const EmailNotVerifiedView = () => {
  const theme = useTheme();
  const imageSrc =
    theme.palette.mode === 'dark'
      ? '/images/emailVerificationDark.svg'
      : '/images/emailVerification.svg';

  const email = useEmailAwaitingVerification();

  if (email == undefined) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  const { t } = useTranslate();

  const resendEmail = useApiMutation({
    url: '/v2/user/send-email-verification',
    method: 'post',
  });

  const setDelayAndStartTimer = (delay: number) => {
    setDelay(delay);
    setEnabled(true);
    startTimer();
  };

  const handleResendEmail = () => {
    resendEmail.mutate(
      {},
      {
        onSuccess: () => {
          setDelayAndStartTimer(30000);

          messageService.success(<T keyName="verify_email_resend_message" />);
        },

        onError: (err) => {
          if (err.data?.message === 'rate_limited') {
            const retryAfter = err.data?.retryAfter;
            setDelayAndStartTimer(retryAfter);
          } else {
            err.handleError?.();
          }
        },
      }
    );
  };

  const [enabled, setEnabled] = useState(false);
  const [delay, setDelay] = useState(0);

  const { startTimer, remainingTime } = useTimerCountdown({
    callback: () => {
      setEnabled(false);
    },
    delay: delay,
    enabled,
  });
  const remainingSeconds = Math.floor(remainingTime / 1000);

  return (
    <StyledWrapper>
      <DashboardPage>
        <BaseView
          windowTitle={t('projects_title')}
          maxWidth={1000}
          allCentered
          hideChildrenOnLoading={false}
          navigationRight={<Usage />}
        >
          <StyledContainer>
            <StyledHeader variant="h4">
              <T keyName="verify_email_check_inbox" />
            </StyledHeader>
            <StyledDescription variant="body1" mb={2}>
              <T
                keyName="verify_email_we_sent_email"
                params={{ email: email, b: <BoldSpan /> }}
              />
            </StyledDescription>
            <StyledImg src={imageSrc} alt="Verify email" />

            <StyledDescription variant="body1">
              {enabled ? (
                <T
                  keyName="verify_email_resend_link_retry_after"
                  params={{
                    seconds: remainingSeconds,
                    link: <StyledLink className="disabled" />,
                  }}
                />
              ) : (
                <T
                  keyName="verify_email_resend_link"
                  params={{
                    link: (
                      <StyledLink
                        className={resendEmail.isLoading ? 'disabled' : ''}
                        data-cy="resend-email-button"
                        onClick={handleResendEmail}
                      />
                    ),
                  }}
                />
              )}
            </StyledDescription>
          </StyledContainer>
        </BaseView>
      </DashboardPage>
    </StyledWrapper>
  );
};
