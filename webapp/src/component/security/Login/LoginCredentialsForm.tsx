import React, { RefObject } from 'react';
import {
  Button,
  Link as MuiLink,
  Typography,
  styled,
  Alert,
} from '@mui/material';
import Box from '@mui/material/Box';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { Key02 } from '@untitled-ui/icons-react';

import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useOAuthServices } from 'tg.hooks/useOAuthServices';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { ApiError } from 'tg.service/http/ApiError';

const StyledInputFields = styled('div')`
  display: grid;
  align-items: start;
  gap: 16px;
  padding-bottom: 32px;
`;

type Credentials = { username: string; password: string };
type LoginViewCredentialsProps = {
  credentialsRef: RefObject<Credentials>;
  onMfaEnabled: () => void;
};

export function LoginCredentialsForm(props: LoginViewCredentialsProps) {
  const remoteConfig = useConfig();
  const { login, loginRedirectSso } = useGlobalActions();
  const isLoading = useGlobalContext(
    (c) =>
      c.auth.loginLoadable.isLoading || c.auth.redirectSsoUrlLoadable.isLoading
  );

  const oAuthServices = useOAuthServices();

  const nativeEnabled = remoteConfig.nativeEnabled;
  const organizationsSsoEnabled =
    remoteConfig.authMethods?.ssoOrganizations.enabled ?? false;
  const globalSsoEnabled = remoteConfig.authMethods?.ssoGlobal.enabled ?? false;
  const hasNonNativeAuthMethods =
    oAuthServices.length > 0 || organizationsSsoEnabled || globalSsoEnabled;
  const noLoginMethods = !nativeEnabled && !hasNonNativeAuthMethods;

  const customLogoUrl = remoteConfig.authMethods?.ssoGlobal.customLogoUrl;
  const customLoginText = remoteConfig.authMethods?.ssoGlobal.customLoginText;
  const loginText = customLoginText ? (
    <span>{customLoginText}</span>
  ) : (
    <T keyName="login_sso" />
  );

  function globalSsoLogin() {
    loginRedirectSso(remoteConfig.authMethods?.ssoGlobal.domain as string);
  }

  return (
    <StandardForm
      initialValues={props.credentialsRef.current!}
      submitButtons={
        <Box>
          <Box display="flex" flexDirection="column" alignItems="stretch">
            {noLoginMethods && (
              <Alert severity="error">
                {/* Did you mess up your configuration? */}
                <T keyName="login_no_login_methods" />
              </Alert>
            )}

            {nativeEnabled && (
              <>
                <LoadingButton
                  loading={isLoading}
                  variant="contained"
                  color="primary"
                  type="submit"
                  data-cy="login-button"
                >
                  <T keyName="login_login_button" />
                </LoadingButton>
                <Box
                  display="flex"
                  justifyContent="center"
                  flexWrap="wrap"
                  mt={1}
                >
                  <MuiLink
                    to={LINKS.RESET_PASSWORD_REQUEST.build()}
                    component={Link}
                  >
                    <Typography variant="body2">
                      <T keyName="login_forgot_your_password" />
                    </Typography>
                  </MuiLink>
                </Box>
              </>
            )}

            {!nativeEnabled && globalSsoEnabled && customLogoUrl && (
              <Box
                display="flex"
                justifyContent="center"
                alignItems="center"
                mb={2}
              >
                <img
                  src={customLogoUrl}
                  alt="Custom Logo"
                  style={{ width: 96, height: 96 }}
                />
              </Box>
            )}

            {nativeEnabled && hasNonNativeAuthMethods && (
              <Box height="0px" mt={5} />
            )}

            {(organizationsSsoEnabled || globalSsoEnabled) && (
              <React.Fragment>
                {organizationsSsoEnabled && (
                  <Button
                    component={Link}
                    to={LINKS.SSO_LOGIN.build()}
                    size="medium"
                    endIcon={<Key02 />}
                    variant="outlined"
                    style={{ marginBottom: '0.5rem' }}
                    color="inherit"
                  >
                    {loginText}
                  </Button>
                )}
                {!organizationsSsoEnabled && (
                  <LoadingButton
                    loading={isLoading}
                    size="medium"
                    endIcon={<Key02 />}
                    variant="outlined"
                    style={{ marginBottom: '0.5rem' }}
                    color="inherit"
                    onClick={globalSsoLogin}
                    data-cy="login-provider"
                    data-cy-provider="sso"
                  >
                    {loginText}
                  </LoadingButton>
                )}
              </React.Fragment>
            )}

            {oAuthServices.map((provider) => (
              <React.Fragment key={provider.id}>
                <Button
                  component="a"
                  href={provider.authenticationUrl}
                  size="medium"
                  endIcon={provider.buttonIcon}
                  variant="outlined"
                  style={{ marginBottom: '0.5rem' }}
                  color="inherit"
                  data-cy="login-provider"
                  data-cy-provider={provider.id}
                >
                  {provider.loginButtonTitle}
                </Button>
              </React.Fragment>
            ))}
          </Box>
        </Box>
      }
      onSubmit={(data) => {
        if (data.username && data.password) {
          props.credentialsRef.current!.username = data.username;
          props.credentialsRef.current!.password = data.password;
          login(data).catch((e: ApiError) => {
            if (e.code === 'mfa_enabled') {
              props.onMfaEnabled();
            }
          });
        }
      }}
    >
      {nativeEnabled && (
        <StyledInputFields>
          <TextField
            name="username"
            label={<T keyName="login_email_label" />}
            minHeight={false}
            autoComplete="username email"
          />
          <TextField
            name="password"
            type="password"
            autoComplete="password"
            label={<T keyName="login_password_label" />}
            minHeight={false}
          />
        </StyledInputFields>
      )}
    </StandardForm>
  );
}
