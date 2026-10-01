import React from 'react';
import { T } from '@tolgee/react';
import { Alert } from '@mui/material';

/**
 * A loopback redirect delivers the token to whatever bound the port, and no `client_id` can prove which program that
 * is. The user who just started a CLI is the only one able to tell it apart from anything else listening.
 */
export const ConsentLocalAppNotice: React.FC = () => {
  return (
    <Alert severity="info" data-cy="oauth2-consent-local-app" sx={{ mb: 2 }}>
      <T
        keyName="oauth2_consent_local_app_notice"
        defaultValue="The access will be handed to an application running on this computer. Continue only if you just started it yourself."
      />
    </Alert>
  );
};
