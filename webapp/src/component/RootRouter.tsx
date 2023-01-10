import { Redirect, Route, Switch } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { PrivateRoute } from './common/PrivateRoute';
import { ProjectsRouter } from 'tg.views/projects/ProjectsRouter';
import { UserSettingsRouter } from 'tg.views/userSettings/UserSettingsRouter';
import { OrganizationsRouter } from 'tg.views/organizations/OrganizationsRouter';
import React, { FC } from 'react';
import { useConfig } from 'tg.globalContext/helpers';
import { GoogleReCaptchaProvider } from 'react-google-recaptcha-v3';
import { Administration } from 'tg.views/administration/Administration';
import { OrganizationRedirectHandler } from './security/OrganizationRedirectHandler';
import { RequirePreferredOrganization } from '../RequirePreferredOrganization';

const LoginRouter = React.lazy(
  () => import(/* webpackChunkName: "login" */ './security/Login/LoginRouter')
);
const SignUpView = React.lazy(
  () =>
    import(
      /* webpackChunkName: "sign-up-view" */ './security/SignUp/SignUpView'
    )
);

const PasswordResetSetView = React.lazy(
  () =>
    import(
      /* webpackChunkName: "reset-password-set-view" */ './security/ResetPasswordSetView'
    )
);
const PasswordResetView = React.lazy(
  () =>
    import(
      /* webpackChunkName: "reset-password-view" */ './security/ResetPasswordView'
    )
);
const AcceptInvitationHandler = React.lazy(
  () =>
    import(
      /* webpackChunkName: "accept-invitation-handler" */ './security/AcceptInvitationHandler'
    )
);

const RecaptchaProvider: FC = (props) => {
  const config = useConfig();
  if (!config.recaptchaSiteKey) {
    return <>{props.children}</>;
  }

  return (
    <GoogleReCaptchaProvider reCaptchaKey={config.recaptchaSiteKey}>
      {props.children}
    </GoogleReCaptchaProvider>
  );
};

export const RootRouter = () => (
  <Switch>
    <Route exact path={LINKS.RESET_PASSWORD_REQUEST.template}>
      <PasswordResetView />
    </Route>
    <Route exact path={LINKS.RESET_PASSWORD_WITH_PARAMS.template}>
      <PasswordResetSetView />
    </Route>
    <Route exact path={LINKS.SIGN_UP.template}>
      <RecaptchaProvider>
        <SignUpView />
      </RecaptchaProvider>
    </Route>
    <Route path={LINKS.LOGIN.template}>
      <LoginRouter />
    </Route>
    <Route path={LINKS.ACCEPT_INVITATION.template}>
      <AcceptInvitationHandler />
    </Route>
    <PrivateRoute path={LINKS.GO_TO_ORGANIZATION.template}>
      <OrganizationRedirectHandler />
    </PrivateRoute>
    <PrivateRoute path={LINKS.USER_SETTINGS.template}>
      <UserSettingsRouter />
    </PrivateRoute>
    <PrivateRoute path={`${LINKS.ADMINISTRATION.template}`}>
      <Administration />
    </PrivateRoute>
    <RequirePreferredOrganization>
      <Switch>
        <PrivateRoute exact path={LINKS.ROOT.template}>
          <Redirect to={LINKS.PROJECTS.template} />
        </PrivateRoute>
        <PrivateRoute path={LINKS.PROJECTS.template}>
          <ProjectsRouter />
        </PrivateRoute>
        <PrivateRoute path={`${LINKS.ORGANIZATIONS.template}`}>
          <OrganizationsRouter />
        </PrivateRoute>
      </Switch>
    </RequirePreferredOrganization>
  </Switch>
);
