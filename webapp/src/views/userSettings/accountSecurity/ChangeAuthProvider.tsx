import React, { FunctionComponent } from 'react';
import { Box, Typography, Button } from '@mui/material';
import { T } from '@tolgee/react';

import { useConfig, useUser } from 'tg.globalContext/helpers';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useOAuthServices } from 'tg.hooks/useOAuthServices';
import { LogIn01 } from '@untitled-ui/icons-react';
import LoadingButton from 'tg.component/common/form/LoadingButton';

export const ChangeAuthProvider: FunctionComponent = () => {
  const { useSsoAuthLinkByDomain } = useGlobalActions();
  const user = useUser();
  const oAuthServices = useOAuthServices();

  const remoteConfig = useConfig();
  const organizationsSsoEnabled =
    remoteConfig.authMethods?.ssoOrganizations.enabled ?? false;
  const globalSsoEnabled = remoteConfig.authMethods?.ssoGlobal.enabled ?? false;
  const ssoEnabled = organizationsSsoEnabled || globalSsoEnabled;

  const ssoUrl = useSsoAuthLinkByDomain(user?.domain || '');

  if (!user) return null;

  return (
    <Box>
      <Typography variant="h6" mt={2}>
        <T keyName="third-party-authentication-options" />
      </Typography>
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        mt={2}
      >
        {ssoEnabled && (
          <LoadingButton
            loading={ssoUrl.isLoading}
            href={ssoUrl.data?.redirectUrl || ''}
            disabled={!ssoUrl.isLoading && !ssoUrl.data?.redirectUrl}
            size="medium"
            endIcon={<LogIn01 />}
            variant="outlined"
            style={{ marginBottom: '0.5rem' }}
            color="inherit"
          >
            <T keyName="login_sso" />
          </LoadingButton>
        )}

        {oAuthServices.map((provider) => (
          // {provider.id === user?.accountType}
          <React.Fragment key={provider.id}>
            <Button
              component="a"
              href={provider.authenticationUrl}
              size="medium"
              endIcon={provider.buttonIcon}
              variant="outlined"
              style={{ marginBottom: '0.5rem' }}
              color="inherit"
            >
              {provider.loginButtonTitle}
            </Button>
          </React.Fragment>
        ))}
      </Box>
    </Box>
  );
};
