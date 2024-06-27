import {Button, styled, Typography, useTheme} from '@mui/material';
import {useApiMutation} from 'tg.service/http/useQueryApi';
import {messageService} from 'tg.service/MessageService';
import {T} from '@tolgee/react';
import React from 'react';

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

export const EmailVerificationPage = ({ email }) => {
  const theme = useTheme();
  const imageSrc =
    theme.palette.mode === 'dark'
      ? '/images/emailVerificationDark.svg'
      : '/images/emailVerification.svg';

  const resendEmail = useApiMutation({
    url: '/v2/user/send-email-verification',
    method: 'post',
  });

  return (
    <StyledContainer>
      <StyledHeader variant="h4">
        <T keyName="verify_email_title" />
      </StyledHeader>
      <StyledDescription variant="body1">
        <T keyName="verify_email_description" params={{ email: email }} />
      </StyledDescription>
      <StyledImg src={imageSrc} alt="Verify email" />
      <StyledHint variant="body2">
        <T keyName="didnt_receive_email_hint" />
      </StyledHint>
      <Button
        variant="contained"
        onClick={() =>
          resendEmail.mutate(
            {},
            {
              onSuccess: () => {
                messageService.success(<T keyName="email_resend_message" />);
              },
            }
          )
        }
        color="primary"
      >
        <T keyName="verify_email_resend_button" />
      </Button>
    </StyledContainer>
  );
};
