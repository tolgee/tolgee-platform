import {default as React, FunctionComponent} from 'react';
import {Route, Switch} from 'react-router-dom';
import {LINKS} from '../../constants/links';
import {LoginView} from './LoginView';
import {OAuthRedirectionHandler} from './OAuthRedirectionHandler';
import {EmailVerificationHandler} from "./EmailVerificationHandler";

interface LoginRouterProps {

}

const LoginRouter: FunctionComponent<LoginRouterProps> = (props) => {
    return (
        <Switch>
            <Route exact path={LINKS.LOGIN.template}>
                <LoginView/>
            </Route>
            <Route path={LINKS.OAUTH_RESPONSE.template}>
                <OAuthRedirectionHandler/>
            </Route>
            <Route path={LINKS.EMAIL_VERIFICATION.template}>
                <EmailVerificationHandler/>
            </Route>
        </Switch>
    );
};
export default LoginRouter;