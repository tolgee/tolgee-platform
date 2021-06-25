import * as Sentry from '@sentry/browser';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { BrowserRouter, Redirect, Route, Switch } from 'react-router-dom';
import { container } from 'tsyringe';
import { LINKS } from '../constants/links';
import { GlobalError } from '../error/GlobalError';
import { useConfig } from '../hooks/useConfig';
import { useUser } from '../hooks/useUser';
import { AppState } from '../store';
import { ErrorActions } from '../store/global/ErrorActions';
import { GlobalActions } from '../store/global/GlobalActions';
import { RedirectionActions } from '../store/global/RedirectionActions';
import { OrganizationsRouter } from '../views/organizations/OrganizationsRouter';
import { ProjectsRouter } from '../views/projects/ProjectsRouter';
import { ApiKeysView } from '../views/userSettings/apiKeys/ApiKeysView';
import { UserProfileView } from '../views/userSettings/UserProfileView';
import ConfirmationDialog from './common/ConfirmationDialog';
import { FullPageLoading } from './common/FullPageLoading';
import { PrivateRoute } from './common/PrivateRoute';
import SnackBar from './common/SnackBar';

const LoginRouter = React.lazy(
  () => import(/* webpackChunkName: "login" */ './security/LoginRouter')
);
const SignUpView = React.lazy(
  () => import(/* webpackChunkName: "sign-up-view" */ './security/SignUpView')
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

const errorActions = container.resolve(ErrorActions);
const redirectionActions = container.resolve(RedirectionActions);

const Redirection = () => {
  const redirectionState = useSelector((state: AppState) => state.redirection);

  useEffect(() => {
    if (redirectionState.to) {
      redirectionActions.redirectDone.dispatch();
    }
  });

  if (redirectionState.to) {
    return <Redirect to={redirectionState.to} />;
  }

  return null;
};

const MandatoryDataProvider = (props: any) => {
  const config = useConfig();
  const userData = useUser();

  useEffect(() => {
    if (config?.clientSentryDsn) {
      Sentry.init({ dsn: config.clientSentryDsn });
      // eslint-disable-next-line no-console
      console.info('Using Sentry!');
    }
  }, [config?.clientSentryDsn]);

  const allowPrivate = useSelector(
    (state: AppState) => state.global.security.allowPrivate
  );

  if (!config || (!userData && allowPrivate && config.authentication)) {
    return <FullPageLoading />;
  } else {
    return props.children;
  }
};

const GlobalConfirmation = () => {
  const state = useSelector(
    (state: AppState) => state.global.confirmationDialog
  );

  const [wasDisplayed, setWasDisplayed] = useState(false);

  const actions = container.resolve(GlobalActions);

  const onCancel = () => {
    state?.onCancel?.();
    actions.closeConfirmation.dispatch();
  };

  const onConfirm = () => {
    state?.onConfirm?.();
    actions.closeConfirmation.dispatch();
  };

  useEffect(() => {
    setWasDisplayed(wasDisplayed || !!state);
  }, [!state]);

  if (!wasDisplayed) {
    return null;
  }

  return (
    <ConfirmationDialog
      open={!!state}
      {...state}
      onCancel={onCancel}
      onConfirm={onConfirm}
    />
  );
};

export class App extends React.Component {
  componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
    errorActions.globalError.dispatch(error as GlobalError);
    throw error;
  }

  render() {
    return (
      <BrowserRouter>
        <Redirection />
        <MandatoryDataProvider>
          <Switch>
            <Route exact path={LINKS.RESET_PASSWORD_REQUEST.template}>
              <PasswordResetView />
            </Route>
            <Route exact path={LINKS.RESET_PASSWORD_WITH_PARAMS.template}>
              <PasswordResetSetView />
            </Route>
            <Route exact path={LINKS.SIGN_UP.template}>
              <SignUpView />
            </Route>
            <Route path={LINKS.LOGIN.template}>
              <LoginRouter />
            </Route>
            <Route path={LINKS.ACCEPT_INVITATION.template}>
              <AcceptInvitationHandler />
            </Route>
            <PrivateRoute exact path={LINKS.ROOT.template}>
              <Redirect to={LINKS.PROJECTS.template} />
            </PrivateRoute>
            <PrivateRoute exact path={LINKS.USER_SETTINGS.template}>
              <UserProfileView />
            </PrivateRoute>
            <PrivateRoute path={LINKS.PROJECTS.template}>
              <ProjectsRouter />
            </PrivateRoute>
            <PrivateRoute path={`${LINKS.USER_API_KEYS.template}`}>
              <ApiKeysView />
            </PrivateRoute>
            <PrivateRoute path={`${LINKS.ORGANIZATIONS.template}`}>
              <OrganizationsRouter />
            </PrivateRoute>
          </Switch>
          <SnackBar />
          <GlobalConfirmation />
        </MandatoryDataProvider>
      </BrowserRouter>
    );
  }
}
