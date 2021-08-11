import { FunctionComponent, useEffect } from 'react';
import { Button } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import GitHubIcon from '@material-ui/icons/GitHub';
import { T } from '@tolgee/react';
import { useSelector } from 'react-redux';
import { Link, Redirect, useHistory } from 'react-router-dom';
import { container } from 'tsyringe';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useConfig } from 'tg.hooks/useConfig';
import { MessageService } from 'tg.service/MessageService';
import { SecurityService } from 'tg.service/SecurityService';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';

import LoadingButton from '../common/form/LoadingButton';
import { StandardForm } from '../common/form/StandardForm';
import { TextField } from '../common/form/fields/TextField';
import { BaseView } from '../layout/BaseView';
import { DashboardPage } from '../layout/DashboardPage';

interface LoginProps {}

const GITHUB_BASE = 'https://github.com/login/oauth/authorize';
const globalActions = container.resolve(GlobalActions);
const securityServiceIns = container.resolve(SecurityService);
const messageService = container.resolve(MessageService);
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

  useEffect(() => {
    if (!authLoading && security.loginErrorCode) {
      messageService.error(<T>{security.loginErrorCode}</T>);
    }
  }, [security.loginErrorCode, authLoading]);

  if (!remoteConfig.authentication || security.allowPrivate) {
    return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
  }

  return (
    <DashboardPage>
      <BaseView lg={4} md={6} sm={8} xs={12}>
        <StandardForm
          initialValues={{ username: '', password: '' }}
          submitButtons={
            <Box ml={-1.5}>
              <Box display="flex" justifyContent="space-between">
                <Box>
                  {security.allowRegistration && (
                    <Button
                      size="large"
                      component={Link}
                      to={LINKS.SIGN_UP.build()}
                    >
                      <T>login_sign_up</T>
                    </Button>
                  )}
                </Box>
                {remoteConfig.passwordResettable && (
                  <Button
                    component={Link}
                    to={LINKS.RESET_PASSWORD_REQUEST.build()}
                  >
                    <T>login_reset_password_button</T>
                  </Button>
                )}
              </Box>
              <Box display="flex">
                <Box flexGrow={1}>
                  {remoteConfig.authMethods?.github?.enabled && (
                    <Button
                      component="a"
                      href={gitHubUrl}
                      size="large"
                      endIcon={<GitHubIcon />}
                    >
                      <T>login_github_login_button</T>
                    </Button>
                  )}
                </Box>
                <Box display="flex" flexGrow={0}>
                  <LoadingButton
                    loading={authLoading}
                    variant="contained"
                    color="primary"
                    type="submit"
                  >
                    <T>login_login_button</T>
                  </LoadingButton>
                </Box>
              </Box>
            </Box>
          }
          onSubmit={(v) => {
            globalActions.login.dispatch(v);
          }}
        >
          <TextField name="username" label={<T>login_username_label</T>} />
          <TextField
            name="password"
            type="password"
            label={<T>login_password_label</T>}
          />
        </StandardForm>
      </BaseView>
    </DashboardPage>
  );
};
