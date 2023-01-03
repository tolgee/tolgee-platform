import { Box, Button, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { ReactComponent as ComeIn } from 'tg.svgs/signup/comeIn.svg';

import { useOAuthServices } from 'tg.hooks/useOAuthServices';

const StyledProviders = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
`;

const StyledComeIn = styled(ComeIn)`
  color: ${({ theme }) => theme.palette.text.secondary};
  margin-top: 30px;
  margin-bottom: 70px;
`;

const StyledMouse = styled('img')`
  position: absolute;
  bottom: 50px;
  right: -75px;
  user-select: none;
  pointer-events: none;
`;

export const SignUpProviders = () => {
  const oAuthServices = useOAuthServices();

  return (
    <StyledProviders>
      {oAuthServices.map((provider, i) => (
        <Button
          key={i}
          component="a"
          href={provider.authenticationUrl}
          size="medium"
          endIcon={provider.buttonIcon}
          variant="outlined"
          style={{ marginBottom: '0.5rem' }}
        >
          <T>{provider.signUpButtonTitle}</T>
        </Button>
      ))}
      <Box display="flex" flexGrow="1" alignItems="end" justifyContent="center">
        <StyledComeIn />
      </Box>
      <StyledMouse src="/images/standardMouse.svg" />
    </StyledProviders>
  );
};
