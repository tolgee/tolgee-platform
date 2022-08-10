import React, { FunctionComponent } from 'react';
import { Alert, Button, Link as MuiLink, Typography } from '@mui/material';
import Box from '@mui/material/Box';
import { T, useTranslate } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { Link, Redirect, useHistory } from 'react-router-dom';
import { container } from 'tsyringe';

import { LINKS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { SecurityService } from 'tg.service/SecurityService';
import { AppState } from 'tg.store/index';

import LoadingButton from '../common/form/LoadingButton';
import { StandardForm } from '../common/form/StandardForm';
import { TextField } from '../common/form/fields/TextField';
import { DashboardPage } from '../layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import {
  gitHubService,
  googleService,
  oauth2Service,
  OAuthService,
} from 'tg.component/security/OAuthService';

interface LoginProps {}

const globalActions = container.resolve(GlobalActions);
const securityServiceIns = container.resolve(SecurityService);
// noinspection JSUnusedLocalSymbols
export const LoginView: FunctionComponent<LoginProps> = (props) => {
  const t = useTranslate();
  const security = useSelector((state: AppState) => state.global.security);
  const authLoading = useSelector(
    (state: AppState) => state.global.authLoading
  );
  const remoteConfig = useConfig();

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

  const history = useHistory();
  if (history.location.state && (history.location.state as any).from) {
    securityServiceIns.saveAfterLoginLink((history.location.state as any).from);
  }

  if (!remoteConfig.authentication || security.allowPrivate) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
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
            initialValues={{ username: '', password: '' }}
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
                  {oAuthServices.map((provider, i) => (
                    <React.Fragment key={i}>
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
            onSubmit={(data) => globalActions.login.dispatch(data)}
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
                <>
                  <MuiLink to={LINKS.SIGN_UP.build()} component={Link}>
                    <Typography variant="caption">
                      <T>login_sign_up</T>
                    </Typography>
                  </MuiLink>
                </>
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
};
