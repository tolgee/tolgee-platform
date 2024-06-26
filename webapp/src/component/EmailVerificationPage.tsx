import {Button, styled, Typography, useTheme} from '@mui/material';

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
    const imageSrc = theme.palette.mode === 'dark'
        ? '/images/emailVerificationDark.svg'
        : '/images/emailVerification.svg';

    return (
        <StyledContainer>
            <StyledHeader variant="h4">
                Verify your email address
            </StyledHeader>
            <StyledDescription variant="body1">
                The final step is to verify your email. After verification, you will be able to use the platform. Please check your inbox at <strong>{email}</strong>.
            </StyledDescription>
            <StyledImg src={imageSrc} alt="Verify email" />
            <StyledHint variant="body2">
                Didn't receive the email?
            </StyledHint>
            <Button
                variant="contained"
                onClick={resendVerificationEmail}
                color="primary"
            >
                RESEND VERIFICATION EMAIL
            </Button>
        </StyledContainer>
    );
};

const resendVerificationEmail = () => {
    console.log('Verification email resent');
};