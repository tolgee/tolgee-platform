import React, { FunctionComponent } from 'react';
import { Box, Typography, Button } from '@mui/material';
import { T } from '@tolgee/react';

import { useConfig, useUser } from 'tg.globalContext/helpers';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useOAuthServices } from 'tg.hooks/useOAuthServices';
import { Key02 } from '@untitled-ui/icons-react';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { LINKS } from 'tg.constants/links';
import { useHistory } from 'react-router-dom';

export const ChangeAuthProvider: FunctionComponent = () => {
  const { useSsoAuthLinkByDomain } = useGlobalActions();
  const user = useUser();
  const oAuthServices = useOAuthServices();
  const history = useHistory();

  const remoteConfig = useConfig();
  const organizationsSsoEnabled =
    remoteConfig.authMethods?.ssoOrganizations.enabled ?? false;
  const globalSsoEnabled = remoteConfig.authMethods?.ssoGlobal.enabled ?? false;
  const ssoEnabled = organizationsSsoEnabled || globalSsoEnabled;

  const ssoUrl = useSsoAuthLinkByDomain(user?.domain || '');

  const rejectChange = useApiMutation({
    url: '/v2/auth-provider',
    method: 'delete',
    fetchOptions: {
      disableAutoErrorHandle: true,
    },
    invalidatePrefix: '/v2/auth-provider',
  });

  if (!user) return null;

  function handleDisconnect() {
    rejectChange.mutate(
      {},
      {
        onSuccess(r) {
          history.push(LINKS.ACCEPT_AUTH_PROVIDER_CHANGE.build());
        },
      }
    );
  }

  const isUserManaged = user.accountType === 'MANAGED';

  return (
    <Box>
      <Typography variant="h6" mt={4}>
        <T keyName="third-party-authentication-options" />
      </Typography>
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        mt={2}
      >
        <Box
          display="flex"
          flexDirection="column"
          alignItems="stretch"
          minWidth={345}
        >
          {ssoEnabled && (ssoUrl.isLoading || ssoUrl.data?.redirectUrl) && (
            <LoadingButton
              loading={ssoUrl.isLoading}
              href={ssoUrl.data?.redirectUrl || ''}
              disabled={
                isUserManaged ||
                (!ssoUrl.isLoading && !ssoUrl.data?.redirectUrl)
              }
              size="medium"
              endIcon={<Key02 />}
              variant="outlined"
              style={{ marginBottom: '0.5rem', marginTop: '0.5rem' }}
              color="inherit"
              data-cy="account-security-provider-connect"
              data-cy-provider="sso"
            >
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
                  style={{ marginBottom: '0.5rem', marginTop: '0.5rem' }}
                  color="success"
                  onClick={handleDisconnect}
                  data-cy="account-security-provider-disconnect"
                  data-cy-provider={provider.id}
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
                  style={{ marginBottom: '0.5rem', marginTop: '0.5rem' }}
                  color="inherit"
                  data-cy="account-security-provider-connect"
                  data-cy-provider={provider.id}
                >
                  {provider.connectButtonTitle}
                </Button>
              )}
            </React.Fragment>
          ))}
        </Box>
      </Box>
    </Box>
  );
};
