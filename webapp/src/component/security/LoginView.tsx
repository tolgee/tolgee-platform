import {default as React, FunctionComponent} from 'react';
import {DashboardPage} from '../layout/DashboardPage';
import {BaseView} from '../layout/BaseView';
import GitHubIcon from '@material-ui/icons/GitHub';
import {Button} from '@material-ui/core';
import {useSelector} from 'react-redux';
import {AppState} from '../../store';
import {LINKS, PARAMS} from '../../constants/links';
import {Link, Redirect, useHistory} from 'react-router-dom';
import {StandardForm} from '../common/form/StandardForm';
import {TextField} from '../common/form/fields/TextField';
import Box from '@material-ui/core/Box';
import {container} from 'tsyringe';
import {GlobalActions} from '../../store/global/globalActions';
import {Alert} from '../common/Alert';
import {securityService} from '../../service/securityService';
import {useConfig} from "../../hooks/useConfig";
import {T} from "@polygloat/react";

interface LoginProps {

}

const globalActions = container.resolve(GlobalActions);
const securityServiceIns = container.resolve(securityService);

export const LoginView: FunctionComponent<LoginProps> = (props) => {

    const security = useSelector((state: AppState) => state.global.security);
    const remoteConfig = useConfig();

    if (!remoteConfig.authentication || security.allowPrivate) {
        return (<Redirect to={LINKS.AFTER_LOGIN.build()}/>);
    }

    const githubBase = 'https://github.com/login/oauth/authorize';
    const githubRedirectUri = LINKS.OAUTH_RESPONSE.buildWithOrigin({[PARAMS.SERVICE_TYPE]: 'github'});
    const clientId = remoteConfig.authMethods.github.clientId;
    const gitHubUrl = githubBase + `?client_id=${clientId}&redirect_uri=${githubRedirectUri}&scope=user%3Aemail`;

    const history = useHistory();
    if (history.location.state && (history.location.state as any).from) {
        securityServiceIns.saveAfterLoginLink((history.location.state as any).from);
    }

    return (
        <DashboardPage>
            <BaseView title={<T>login_title</T>} lg={4} md={6} sm={8} xs={12}>
                {security.loginErrorCode &&
                <Box mt={1} ml={-2} mr={-2}>
                    <Alert severity="error"><T>{security.loginErrorCode}</T></Alert>
                </Box>
                }
                <StandardForm initialValues={{username: '', password: ''}}
                              submitButtons={
                                  <Box ml={-1.5}>
                                      <Box display="flex" justifyContent="space-between">
                                          <Box>
                                              {security.allowRegistration &&
                                              <Button size="large" component={Link} to={LINKS.SIGN_UP.build()}>
                                                  <T>login_sign_up</T>
                                              </Button>
                                              }
                                          </Box>
                                          {remoteConfig.passwordResettable &&
                                          <Button component={Link} to={LINKS.RESET_PASSWORD_REQUEST.build()}>
                                              <T>login_reset_password_button</T>
                                          </Button>
                                          }
                                      </Box>
                                      <Box display="flex">
                                          <Box flexGrow={1}>
                                              {remoteConfig.authMethods.github?.enabled &&
                                              (
                                                  <Button component="a" href={gitHubUrl} size="large" endIcon={<GitHubIcon/>}>
                                                      <T>login_github_login_button</T>
                                                  </Button>
                                              )}
                                          </Box>
                                          <Box display="flex" flexGrow={0}>
                                              <Button variant="contained" color="primary" type="submit"><T>login_login_button</T></Button>
                                          </Box>
                                      </Box>
                                  </Box>}
                              onSubmit={(v) => {
                                  globalActions.login.dispatch(v);
                              }}>
                    <TextField name="username" label={<T>login_username_label</T>}/>
                    <TextField name="password" type="password" label={<T>login_password_label</T>}/>
                </StandardForm>
            </BaseView>
        </DashboardPage>
    );
};
