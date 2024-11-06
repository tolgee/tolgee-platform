import React, { RefObject } from 'react';
import { Button, Link as MuiLink, Typography, styled } from '@mui/material';
import Box from '@mui/material/Box';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import LoginIcon from '@mui/icons-material/Login';

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
import { useSsoService } from 'tg.component/security/SsoService';

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
  const { login } = useGlobalActions();
  const isLoading = useGlobalContext((c) => c.auth.loginLoadable.isLoading);

  const oAuthServices = useOAuthServices();
  // TODO: do I need to do something to monitor the state of the redirectLoadable?
  const { handleRedirect, redirectLoadable } = useSsoService();

  return (
    <StandardForm
      initialValues={props.credentialsRef.current!}
      submitButtons={
        <Box>
          <Box display="flex" flexDirection="column" alignItems="stretch">
            {remoteConfig.nativeEnabled && (
              <LoadingButton
                loading={isLoading}
                variant="contained"
                color="primary"
                type="submit"
                data-cy="login-button"
              >
                <T keyName="login_login_button" />
              </LoadingButton>
            )}

            <Box display="flex" justifyContent="center" flexWrap="wrap" mt={1}>
              {remoteConfig.passwordResettable && (
                <MuiLink
                  to={LINKS.RESET_PASSWORD_REQUEST.build()}
                  component={Link}
                >
                  <Typography variant="body2">
                    <T keyName="login_forgot_your_password" />
                  </Typography>
                </MuiLink>
              )}
            </Box>

            {!remoteConfig.nativeEnabled && (
              <Button
                component={Link}
                to={LINKS.SSO_LOGIN.build()}
                size="medium"
                endIcon={
                  remoteConfig.authMethods?.sso.customLogoUrl ? (
                    <img
                      src={remoteConfig.authMethods?.sso.customLogoUrl}
                      alt="Custom Logo"
                      style={{ width: 24, height: 24 }}
                    />
                  ) : (
                    <LoginIcon />
                  )
                }
                variant="outlined"
                style={{ marginBottom: '0.5rem' }}
                color="inherit"
              >
                {remoteConfig.authMethods?.sso.customLoginText ? (
                  <span>{remoteConfig.authMethods?.sso.customLoginText}</span>
                ) : (
                  <T keyName="login_sso" />
                )}
              </Button>
            )}

            {
              (oAuthServices.length > 0 || remoteConfig.authMethods?.sso.globalEnabled || remoteConfig.authMethods?.sso.enabled) &&
              <Box
                height="0px"
                mt={5}
              />
            }
            {remoteConfig.nativeEnabled &&
              !remoteConfig.authMethods?.sso.globalEnabled && (
                <Button
                  component={Link}
                  to={LINKS.SSO_LOGIN.build()}
                  size="medium"
                  endIcon={<LoginIcon />}
                  variant="outlined"
                  style={{ marginBottom: '0.5rem', marginTop: '1rem' }}
                  color="inherit"
                >
                  <T keyName="login_sso" />
                </Button>
              )}
            {remoteConfig.nativeEnabled &&
              remoteConfig.authMethods?.sso.globalEnabled && (
                <LoadingButton
                  loading={redirectLoadable.isLoading}
                  size="medium"
                  endIcon={<LoginIcon />}
                  variant="outlined"
                  style={{ marginBottom: '0.5rem', marginTop: '1rem' }}
                  color="inherit"
                  onClick={async (data) => {
                    await handleRedirect(
                      remoteConfig.authMethods?.sso.domain as string
                    );
                  }}
                >
                  <T keyName="login_sso" />
                </LoadingButton>
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
      {remoteConfig.nativeEnabled && (
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
