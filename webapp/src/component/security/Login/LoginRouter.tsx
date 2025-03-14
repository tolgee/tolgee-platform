import { default as React, FunctionComponent } from 'react';
import { Route, Switch } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';

import { LoginView } from './LoginView';
import { EmailVerificationHandler } from './EmailVerificationHandler';
import { OAuthRedirectionHandler } from './OAuthRedirectionHandler';
import { PublicOnlyRoute } from 'tg.component/common/PublicOnlyRoute';

interface LoginRouterProps {}

const LoginRouter: FunctionComponent<LoginRouterProps> = (props) => {
  return (
    <Switch>
      <PublicOnlyRoute exact path={LINKS.LOGIN.template}>
        <LoginView />
      </PublicOnlyRoute>
      <Route path={LINKS.OAUTH_RESPONSE.template}>
        <OAuthRedirectionHandler />
      </Route>
      <Route path={LINKS.EMAIL_VERIFICATION.template}>
        <EmailVerificationHandler />
      </Route>
    </Switch>
  );
};
export default LoginRouter;
