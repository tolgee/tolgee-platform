import React, { FunctionComponent } from 'react';
import { Box, Typography, Button } from '@mui/material';
import { T } from '@tolgee/react';

import { useConfig, useUser } from 'tg.globalContext/helpers';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useOAuthServices } from 'tg.hooks/useOAuthServices';
import { Key02 } from '@untitled-ui/icons-react';
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

  const isUserManaged = user.accountType === 'MANAGED';

  return (
    <Box>
      <Typography variant="h6" mt={2}>
        <T keyName="third-party-authentication-options" />
      </Typography>
      {/* TODO: Show info card when user is managed explaining why no third-party provider changes are available */}
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
            disabled={
              isUserManaged || (!ssoUrl.isLoading && !ssoUrl.data?.redirectUrl)
            }
            size="medium"
            endIcon={<Key02 />}
            variant="outlined"
            style={{ marginBottom: '0.5rem' }}
            color="inherit"
          >
            {/* TODO: Maybe hover hint explaining when is SSO available? */}
            <T keyName="connect_sso" />
          </LoadingButton>
        )}

        {oAuthServices.map((provider) => (
          <React.Fragment key={provider.id}>
            {provider.id.toUpperCase() === user?.thirdPartyAuthType ? (
              <Button
                disabled={isUserManaged}
                size="medium"
                endIcon={provider.buttonIcon}
                variant="outlined"
                style={{ marginBottom: '0.5rem' }}
                color="success"
              >
                {provider.disconnectButtonTitle}
              </Button>
            ) : (
              <Button
                disabled={isUserManaged}
                component="a"
                href={provider.authenticationUrl}
                size="medium"
                endIcon={provider.buttonIcon}
                variant="outlined"
                style={{ marginBottom: '0.5rem' }}
                color="inherit"
              >
                {provider.connectButtonTitle}
              </Button>
            )}
          </React.Fragment>
        ))}
      </Box>
    </Box>
  );
};
