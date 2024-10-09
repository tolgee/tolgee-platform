import React, { FC } from 'react';
import { Redirect, Route, Switch } from 'react-router-dom';
import { GoogleReCaptchaProvider } from 'react-google-recaptcha-v3';

import { LINKS } from 'tg.constants/links';
import { ProjectsRouter } from 'tg.views/projects/ProjectsRouter';
import { UserSettingsRouter } from 'tg.views/userSettings/UserSettingsRouter';
import { OrganizationsRouter } from 'tg.views/organizations/OrganizationsRouter';
import { useConfig } from 'tg.globalContext/helpers';
import { AdministrationView } from 'tg.views/administration/AdministrationView';
import { RootView } from 'tg.views/RootView';
import { MyTasksView } from 'tg.ee/task/views/myTasks/MyTasksView';

import { PrivateRoute } from './common/PrivateRoute';
import { OrganizationBillingRedirect } from './security/OrganizationBillingRedirect';
import { RequirePreferredOrganization } from '../RequirePreferredOrganization';
import { HelpMenu } from './HelpMenu';
import { PublicOnlyRoute } from './common/PublicOnlyRoute';
import { PreferredOrganizationRedirect } from './security/PreferredOrganizationRedirect';

const LoginRouter = React.lazy(
  () => import(/* webpackChunkName: "login" */ './security/Login/LoginRouter')
);

const SlackConnectView = React.lazy(
  () =>
    import(
      /* webpackChunkName: "slack-connect-view" */ './slack/SlackConnectView'
    )
);

const SlackConnectedView = React.lazy(
  () =>
    import(
      /* webpackChunkName: "slack-connected-view" */ './slack/SlackConnectedView'
    )
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
    <GoogleReCaptchaProvider
      reCaptchaKey={config.recaptchaSiteKey}
      useRecaptchaNet={true}
    >
      {props.children}
    </GoogleReCaptchaProvider>
  );
};

export const RootRouter = () => (
  <Switch>
    <PrivateRoute exact path={LINKS.SLACK_CONNECT.template}>
      <SlackConnectView />
    </PrivateRoute>
    <PrivateRoute exact path={LINKS.SLACK_CONNECTED.template}>
      <SlackConnectedView />
    </PrivateRoute>
    <Route exact path={LINKS.RESET_PASSWORD_REQUEST.template}>
      <PasswordResetView />
    </Route>
    <Route exact path={LINKS.RESET_PASSWORD_WITH_PARAMS.template}>
      <PasswordResetSetView />
    </Route>
    <PublicOnlyRoute exact path={LINKS.SIGN_UP.template}>
      <RecaptchaProvider>
        <SignUpView />
      </RecaptchaProvider>
    </PublicOnlyRoute>
    <Route path={LINKS.LOGIN.template}>
      <LoginRouter />
    </Route>
    <Route path={LINKS.ACCEPT_INVITATION.template}>
      <AcceptInvitationHandler />
    </Route>
    <PrivateRoute path={LINKS.GO_TO_CLOUD_BILLING.template}>
      <OrganizationBillingRedirect selfHosted={false} />
    </PrivateRoute>
    <PrivateRoute path={LINKS.GO_TO_SELF_HOSTED_BILLING.template}>
      <OrganizationBillingRedirect selfHosted={true} />
    </PrivateRoute>
    <PrivateRoute path={LINKS.GO_TO_PREFERRED_ORGANIZATION.template}>
      <PreferredOrganizationRedirect />
    </PrivateRoute>
    <PrivateRoute path={LINKS.USER_SETTINGS.template}>
      <UserSettingsRouter />
    </PrivateRoute>
    <PrivateRoute path={`${LINKS.ADMINISTRATION.template}`}>
      <AdministrationView />
    </PrivateRoute>
    <PrivateRoute exact path={LINKS.MY_TASKS.template}>
      <MyTasksView />
    </PrivateRoute>

    <RequirePreferredOrganization>
      <Switch>
        <PrivateRoute exact path={LINKS.ROOT.template}>
          <RootView />
        </PrivateRoute>
        <PrivateRoute exact path={LINKS.PROJECTS.template}>
          <Redirect to={LINKS.ROOT.template} />
        </PrivateRoute>
        <PrivateRoute path={LINKS.PROJECTS.template}>
          <ProjectsRouter />
        </PrivateRoute>
        <PrivateRoute path={`${LINKS.ORGANIZATIONS.template}`}>
          <OrganizationsRouter />
        </PrivateRoute>
      </Switch>
      <HelpMenu />
    </RequirePreferredOrganization>
  </Switch>
);
