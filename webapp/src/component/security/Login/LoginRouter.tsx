import { default as React, FunctionComponent } from 'react';
import { Route, Switch } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';

import { LoginView } from './LoginView';
import { EmailVerificationHandler } from './EmailVerificationHandler';
import { OAuthRedirectionHandler } from './OAuthRedirectionHandler';
import { PublicOnlyRoute } from 'tg.component/common/PublicOnlyRoute';
import { SsoRedirectionHandler } from 'tg.component/security/Sso/SsoRedirectionHandler';
import { ChangeAuthProviderView } from 'tg.component/security/ChangeAuthProviderView';

interface LoginRouterProps {}

const LoginRouter: FunctionComponent<LoginRouterProps> = (props) => {
  return (
    <Switch>
      <PublicOnlyRoute exact path={LINKS.LOGIN.template}>
        <LoginView />
      </PublicOnlyRoute>
      <PublicOnlyRoute path={LINKS.OAUTH_RESPONSE.template}>
        <OAuthRedirectionHandler />
      </PublicOnlyRoute>
      <PublicOnlyRoute path={LINKS.CHANGE_AUTH_PROVIDER.template}>
        <ChangeAuthProviderView />
      </PublicOnlyRoute>
      <PublicOnlyRoute path={LINKS.OPENID_RESPONSE.template}>
        <SsoRedirectionHandler />
      </PublicOnlyRoute>
      <Route path={LINKS.EMAIL_VERIFICATION.template}>
        <EmailVerificationHandler />
      </Route>
    </Switch>
  );
};
export default LoginRouter;
