import { Button, styled, Typography, useTheme } from '@mui/material';
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
`;

const StyledHint = styled(Typography)`
  margin-bottom: 20px;
  font-weight: bold;
`;

const StyledImg = styled('img')`
  margin-top: 20px;
  margin-bottom: 30px;
`;

const StyledEnabled = styled('span')`
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
              <T keyName="verify_email_title" />
            </StyledHeader>
            <StyledDescription variant="body1" mb={2}>
              <T
                keyName="verify_email_description"
                params={{ email: email, b: <StyledEnabled /> }}
              />
            </StyledDescription>
            <StyledImg src={imageSrc} alt="Verify email" />
            <StyledHint variant="body2">
              <T keyName="verify_email_didnt_receive_email_hint" />
            </StyledHint>
            <Button
              variant="contained"
              data-cy="resend-email-button"
              onClick={() =>
                resendEmail.mutate(
                  {},
                  {
                    onSuccess: () => {
                      messageService.success(
                        <T keyName="verify_email_resend_message" />
                      );
                    },
                  }
                )
              }
              color="primary"
            >
              <T keyName="verify_email_resend_button" />
            </Button>
          </StyledContainer>
        </BaseView>
      </DashboardPage>
    </StyledWrapper>
  );
};
