import { Box, Button, styled, useMediaQuery } from '@mui/material';

import { useOAuthServices } from 'tg.hooks/useOAuthServices';
import { MouseIllustration } from '../MouseIllustration';
import { TolgeeMore } from '../TolgeeMore';
import { SPLIT_CONTENT_BREAK_POINT } from 'tg.component/layout/CompactView';

const StyledProviders = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
`;

export const SignUpProviders = () => {
  const oAuthServices = useOAuthServices();
  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

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
          color="inherit"
          style={{ marginBottom: '0.5rem' }}
        >
          {provider.signUpButtonTitle}
        </Button>
      ))}
      {!isSmall && (
        <Box my={5}>
          <MouseIllustration />
        </Box>
      )}
      <TolgeeMore />
    </StyledProviders>
  );
};
