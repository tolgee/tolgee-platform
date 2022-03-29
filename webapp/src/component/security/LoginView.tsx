import { FunctionComponent } from 'react';
import { Button, Typography, Link as MuiLink } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import GitHubIcon from '@material-ui/icons/GitHub';
import { T } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { Link, Redirect, useHistory } from 'react-router-dom';
import { container } from 'tsyringe';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useConfig } from 'tg.hooks/useConfig';
import { SecurityService } from 'tg.service/SecurityService';
import { AppState } from 'tg.store/index';

import LoadingButton from '../common/form/LoadingButton';
import { StandardForm } from '../common/form/StandardForm';
import { TextField } from '../common/form/fields/TextField';
import { DashboardPage } from '../layout/DashboardPage';
import { CompactView } from 'tg.component/layout/CompactView';
import { Alert } from '@material-ui/lab';
import { GlobalActions } from 'tg.store/global/GlobalActions';

interface LoginProps {}

const GITHUB_BASE = 'https://github.com/login/oauth/authorize';
const globalActions = container.resolve(GlobalActions);
const securityServiceIns = container.resolve(SecurityService);
// noinspection JSUnusedLocalSymbols
export const LoginView: FunctionComponent<LoginProps> = (props) => {
  const security = useSelector((state: AppState) => state.global.security);
  const authLoading = useSelector(
    (state: AppState) => state.global.authLoading
  );
  const remoteConfig = useConfig();

  const githubRedirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({
    [PARAMS.SERVICE_TYPE]: 'github',
  });
  const clientId = remoteConfig.authMethods!.github.clientId;
  const gitHubUrl =
    GITHUB_BASE +
    `?client_id=${clientId}&redirect_uri=${githubRedirectUri}&scope=user%3Aemail`;

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
        alerts={
          security.loginErrorCode &&
          !authLoading && (
            <Alert severity="error">
              <T>{security.loginErrorCode}</T>
            </Alert>
          )
        }
        title={<T>login_title</T>}
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
                  >
                    <T>login_login_button</T>
                  </LoadingButton>

                  {remoteConfig.authMethods?.github?.enabled && (
                    <>
                      <Box
                        height="1px"
                        bgcolor="lightgray"
                        marginY={4}
                        marginX={-1}
                      />
                      <Button
                        component="a"
                        href={gitHubUrl}
                        size="medium"
                        endIcon={<GitHubIcon />}
                        variant="outlined"
                      >
                        <T>login_github_login_button</T>
                      </Button>
                    </>
                  )}
                </Box>
              </Box>
            }
            onSubmit={(data) => globalActions.login.dispatch(data)}
          >
            <TextField name="username" label={<T>login_email_label</T>} />
            <TextField
              name="password"
              type="password"
              label={<T>login_password_label</T>}
            />
          </StandardForm>
        }
        footer={
          <Box display="flex" justifyContent="space-between" flexWrap="wrap">
            <Box>
              {security.allowRegistration && (
                <>
                  <Link to={LINKS.SIGN_UP.build()} component={MuiLink}>
                    <Typography variant="caption">
                      <T>login_sign_up</T>
                    </Typography>
                  </Link>
                </>
              )}
            </Box>
            {remoteConfig.passwordResettable && (
              <Link
                to={LINKS.RESET_PASSWORD_REQUEST.build()}
                component={MuiLink}
              >
                <Typography variant="caption">
                  <T>login_reset_password_button</T>
                </Typography>
              </Link>
            )}
          </Box>
        }
      />
    </DashboardPage>
  );
};
