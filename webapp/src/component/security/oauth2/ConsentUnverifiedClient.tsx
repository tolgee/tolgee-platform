import React from 'react';
import { T } from '@tolgee/react';
import { Alert, Box, styled } from '@mui/material';

const StyledName = styled('div')`
  overflow-wrap: anywhere;
  unicode-bidi: isolate;
  /* Stacked combining marks render above their line box; without a bound they paint over the warning above. */
  max-height: ${({ theme }) => theme.spacing(6)};
  overflow: hidden;
`;

const StyledOrigin = styled('div')`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: ${({ theme }) => theme.typography.body2.fontSize}px;
  overflow-wrap: anywhere;
`;

type Props = {
  appName: string;
  clientOrigin?: string;
};

/**
 * No logo element: loading an image from the app's own server would tell that server who opened the consent screen
 * and when.
 */
export const ConsentUnverifiedClient: React.FC<Props> = ({
  appName,
  clientOrigin,
}) => {
  return (
    <Box data-cy="oauth2-consent-unverified" sx={{ mb: 2 }}>
      <Alert severity="warning" data-cy="oauth2-consent-unverified-warning">
        <T
          keyName="oauth2_consent_unverified_warning"
          defaultValue="Tolgee hasn't verified this app. Only continue if you opened this request from an app you trust."
        />
      </Alert>
      <Box sx={{ mt: 2 }}>
        {clientOrigin && (
          <StyledOrigin data-cy="oauth2-consent-origin">
            {clientOrigin}
          </StyledOrigin>
        )}
        <StyledName data-cy="oauth2-consent-app-name">{appName}</StyledName>
      </Box>
    </Box>
  );
};
