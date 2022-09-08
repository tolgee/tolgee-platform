import React, {
  FunctionComponent,
  RefObject,
  useEffect,
  useRef,
  useState,
} from 'react';
import { Alert, Button, Link as MuiLink, Typography } from '@mui/material';
import Box from '@mui/material/Box';
import { T, useTranslate } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { Link, Redirect, useHistory } from 'react-router-dom';
import { container } from 'tsyringe';

import { components } from 'tg.service/apiSchema.generated';
import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { SecurityService } from 'tg.service/SecurityService';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import {
  gitHubService,
  googleService,
  oauth2Service,
  OAuthService,
} from 'tg.component/security/OAuthService';

interface LoginProps {}

type LoginRequestDto = components['schemas']['LoginRequest'];

const globalActions = container.resolve(GlobalActions);
const securityServiceIns = container.resolve(SecurityService);

type Credentials = { username: string; password: string };
type LoginViewCredentialsProps = {
  credentialsRef: RefObject<Credentials>;
  onMfaEnabled: () => void;
};
type LoginViewTotpProps = {
  credentialsRef: RefObject<Credentials>;
  onMfaCancel: () => void;
};

export function LoginViewCredentials(props: LoginViewCredentialsProps) {
  const t = useTranslate();
  const remoteConfig = useConfig();
  const security = useSelector((state: AppState) => state.global.security);
  const authLoading = useSelector(
    (state: AppState) => state.global.authLoading
  );

  useEffect(() => {
    if (security.loginErrorCode === 'mfa_enabled') {
      security.loginErrorCode = null;
      props.onMfaEnabled();
    }
  }, [security.loginErrorCode]);

  const oAuthServices: OAuthService[] = [];
  const githubConfig = remoteConfig.authMethods?.github;
  const googleConfig = remoteConfig.authMethods?.google;
  const oauth2Config = remoteConfig.authMethods?.oauth2;
  if (githubConfig?.enabled && githubConfig.clientId) {
    oAuthServices.push(gitHubService(githubConfig.clientId));
  }
  if (googleConfig?.enabled && googleConfig.clientId) {
    oAuthServices.push(googleService(googleConfig.clientId));
  }
  if (
    oauth2Config?.enabled &&
    oauth2Config?.clientId &&
    oauth2Config.scopes &&
    oauth2Config?.authorizationUrl
  ) {
    oAuthServices.push(
      oauth2Service(
        oauth2Config.clientId,
        oauth2Config.authorizationUrl,
        oauth2Config.scopes
      )
    );
  }

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
                        <T>{provider.buttonLabelTranslationKey}</T>
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
              {security.allowRegistration && (
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

export function LoginViewTotp(props: LoginViewTotpProps) {
  const t = useTranslate();
  const security = useSelector((state: AppState) => state.global.security);
  const authLoading = useSelector(
    (state: AppState) => state.global.authLoading
  );

  return (
    <DashboardPage>
      <CompactView
        windowTitle={t('account-security-mfa')}
        title={t('account-security-mfa')}
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
            initialValues={
              {
                username: props.credentialsRef.current!.username,
                password: props.credentialsRef.current!.password,
                otp: '',
              } as LoginRequestDto
            }
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
                </Box>
              </Box>
            }
            onSubmit={(data) => globalActions.login.dispatch(data)}
          >
            <TextField
              name="otp"
              label={<T>account-security-mfa-otp-code</T>}
              variant="standard"
            />
          </StandardForm>
        }
        footer={
          <Box display="flex" justifyContent="flex-end">
            <MuiLink onClick={() => props.onMfaCancel()} component={'button'}>
              <Typography variant="caption">
                <T>global_cancel_button</T>
              </Typography>
            </MuiLink>
          </Box>
        }
      />
    </DashboardPage>
  );
}

// noinspection JSUnusedLocalSymbols
export const LoginView: FunctionComponent<LoginProps> = (props) => {
  const credentialsRef = useRef({ username: '', password: '' });
  const [mfaRequired, setMfaRequired] = useState(false);

  const security = useSelector((state: AppState) => state.global.security);
  const remoteConfig = useConfig();
  const history = useHistory();

  if (history.location.state && (history.location.state as any).from) {
    securityServiceIns.saveAfterLoginLink((history.location.state as any).from);
  }

  if (!remoteConfig.authentication || security.allowPrivate) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  if (mfaRequired) {
    return (
      <LoginViewTotp
        credentialsRef={credentialsRef}
        onMfaCancel={() => {
          credentialsRef.current!.password = '';
          setMfaRequired(false);
        }}
      />
    );
  }

  return (
    <LoginViewCredentials
      credentialsRef={credentialsRef}
      onMfaEnabled={() => setMfaRequired(true)}
    />
  );
};
