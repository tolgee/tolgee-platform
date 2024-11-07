import React, { RefObject } from 'react';
import { Button, Link as MuiLink, Typography, styled } from '@mui/material';
import Box from '@mui/material/Box';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { LogIn01 } from '@untitled-ui/icons-react';

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
  const ssoEnabled = remoteConfig.authMethods?.sso.enabled ?? false;
  const globalSsoEnabled = remoteConfig.authMethods?.sso.globalEnabled ?? false;
  const hasNonNativeAuthMethods =
    oAuthServices.length > 0 || ssoEnabled || globalSsoEnabled;
  const noLoginMethods = !nativeEnabled && !hasNonNativeAuthMethods;

  const customLogoUrl = remoteConfig.authMethods?.sso.customLogoUrl;
  // TODO: the custom logo is just displayed on button, is that expected?
  const logoIcon = customLogoUrl ? (
    <img
      src={customLogoUrl}
      alt="Custom Logo"
      style={{ width: 24, height: 24 }}
    />
  ) : (
    <LogIn01 />
  );
  const customLoginText = remoteConfig.authMethods?.sso.customLoginText;
  const loginText = customLoginText ? (
    <span>{customLoginText}</span>
  ) : (
    <T keyName="login_sso" />
  );

  function globalSsoLogin() {
    loginRedirectSso(remoteConfig.authMethods?.sso.domain as string);
  }

  return (
    <StandardForm
      initialValues={props.credentialsRef.current!}
      submitButtons={
        <Box>
          <Box display="flex" flexDirection="column" alignItems="stretch">
            {noLoginMethods && (
              <Typography variant="body1" color="error">
                {/* Did you mess up your configuration? */}
                <T keyName="login_no_login_methods" />
              </Typography>
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

            {nativeEnabled && hasNonNativeAuthMethods && (
              <Box height="0px" mt={5} />
            )}

            {ssoEnabled && (
              <React.Fragment>
                {!globalSsoEnabled && (
                  <Button
                    component={Link}
                    to={LINKS.SSO_LOGIN.build()}
                    size="medium"
                    endIcon={logoIcon}
                    variant="outlined"
                    style={{ marginBottom: '0.5rem' }}
                    color="inherit"
                  >
                    {loginText}
                  </Button>
                )}
                {globalSsoEnabled && (
                  <LoadingButton
                    loading={isLoading}
                    size="medium"
                    endIcon={logoIcon}
                    variant="outlined"
                    style={{ marginBottom: '0.5rem' }}
                    color="inherit"
                    onClick={globalSsoLogin}
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
          />
          <TextField
            name="password"
            type="password"
            label={<T keyName="login_password_label" />}
            minHeight={false}
          />
        </StyledInputFields>
      )}
    </StandardForm>
  );
}
