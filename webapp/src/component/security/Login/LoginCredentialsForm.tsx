import React, { RefObject, useEffect } from 'react';
import { Alert, Button, Link as MuiLink, Typography } from '@mui/material';
import Box from '@mui/material/Box';
import { T, useTranslate } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import { container } from 'tsyringe';

import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import { useOAuthServices } from 'tg.hooks/useOAuthServices';

const globalActions = container.resolve(GlobalActions);

type Credentials = { username: string; password: string };
type LoginViewCredentialsProps = {
  credentialsRef: RefObject<Credentials>;
  onMfaEnabled: () => void;
};

export function LoginCredentialsForm(props: LoginViewCredentialsProps) {
  const { t } = useTranslate();
  const remoteConfig = useConfig();
  const security = useSelector((state: AppState) => state.global.security);

  const authLoading = useSelector(
    (state: AppState) => state.global.authLoading
  );

  const registrationsAllowed =
    remoteConfig.allowRegistrations || security.allowRegistration;

  useEffect(() => {
    if (security.loginErrorCode === 'mfa_enabled') {
      security.loginErrorCode = null;
      props.onMfaEnabled();
    }
  }, [security.loginErrorCode]);

  const oAuthServices = useOAuthServices();

  return (
    <DashboardPage>
      <CompactView
        windowTitle={t('login_title')}
        title={t('login_title')}
        alerts={
          security.loginErrorCode &&
          !authLoading && (
            <Alert severity="error">
              <T>{security.loginErrorCode}</T>
            </Alert>
          )
        }
        content={
          <StandardForm
            initialValues={props.credentialsRef.current!}
            submitButtons={
              <Box mt={2}>
                <Box display="flex" flexDirection="column" alignItems="stretch">
                  <LoadingButton
                    loading={authLoading}
                    variant="contained"
                    color="primary"
                    type="submit"
                    data-cy="login-button"
                  >
                    <T>login_login_button</T>
                  </LoadingButton>

                  {oAuthServices.length > 0 && (
                    <Box
                      height="1px"
                      bgcolor="lightgray"
                      marginY={4}
                      marginX={-1}
                    />
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
                      >
                        <T>{provider.loginButtonTitle}</T>
                      </Button>
                    </React.Fragment>
                  ))}
                </Box>
              </Box>
            }
            onSubmit={(data) => {
              props.credentialsRef.current!.username = data.username;
              props.credentialsRef.current!.password = data.password;
              globalActions.login.dispatch(data);
            }}
          >
            <TextField
              name="username"
              label={<T>login_email_label</T>}
              variant="standard"
            />
            <TextField
              name="password"
              type="password"
              label={<T>login_password_label</T>}
              variant="standard"
            />
          </StandardForm>
        }
        footer={
          <Box display="flex" justifyContent="space-between" flexWrap="wrap">
            <Box>
              {registrationsAllowed && (
                <MuiLink to={LINKS.SIGN_UP.build()} component={Link}>
                  <Typography variant="caption">
                    <T>login_sign_up</T>
                  </Typography>
                </MuiLink>
              )}
            </Box>
            {remoteConfig.passwordResettable && (
              <MuiLink
                to={LINKS.RESET_PASSWORD_REQUEST.build()}
                component={Link}
              >
                <Typography variant="caption">
                  <T>login_reset_password_button</T>
                </Typography>
              </MuiLink>
            )}
          </Box>
        }
      />
    </DashboardPage>
  );
}
