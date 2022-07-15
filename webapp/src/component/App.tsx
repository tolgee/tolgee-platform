import React, { FC, useEffect, useState } from 'react';
import * as Sentry from '@sentry/browser';
import { useSelector } from 'react-redux';
import { Redirect, Route, Switch, BrowserRouter } from 'react-router-dom';
import { container } from 'tsyringe';
import { Helmet } from 'react-helmet';
import { useTheme } from '@mui/material';
import { GoogleReCaptchaProvider } from 'react-google-recaptcha-v3';
import type API from '@openreplay/tracker';

import { UserSettingsRouter } from 'tg.views/userSettings/UserSettingsRouter';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import {
  useConfig,
  useOrganizationUsage,
  usePreferredOrganization,
  useUser,
} from 'tg.globalContext/helpers';
import { LINKS } from '../constants/links';
import { GlobalError } from '../error/GlobalError';
import { AppState } from '../store';
import { ErrorActions } from '../store/global/ErrorActions';
import { GlobalActions } from '../store/global/GlobalActions';
import { RedirectionActions } from '../store/global/RedirectionActions';
import { OrganizationsRouter } from '../views/organizations/OrganizationsRouter';
import { ProjectsRouter } from '../views/projects/ProjectsRouter';
import ConfirmationDialog from './common/ConfirmationDialog';
import { PrivateRoute } from './common/PrivateRoute';
import SnackBar from './common/SnackBar';
import { Chatwoot } from './Chatwoot';
import { useGlobalLoading } from './GlobalLoading';
import { PlanLimitPopover } from './billing/PlanLimitPopover';

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
  const allowPrivate = useSelector(
    (v: AppState) => v.global.security.allowPrivate
  );
  const userData = useUser();
  const isLoading = useGlobalContext((v) => v.isLoading);
  const isFetching = useGlobalContext((v) => v.isFetching);
  const { preferredOrganization } = usePreferredOrganization();
  const [openReplayTracker, setOpenReplayTracker] = useState(
    undefined as undefined | API
  );

  useEffect(() => {
    if (config?.clientSentryDsn) {
      Sentry.init({ dsn: config.clientSentryDsn });
      // eslint-disable-next-line no-console
      console.info('Using Sentry!');
    }
  }, [config?.clientSentryDsn]);

  useEffect(() => {
    const openReplayApiKey = config?.openReplayApiKey;
    if (openReplayApiKey && !window.openReplayTracker) {
      import('@openreplay/tracker').then(({ default: Tracker }) => {
        window.openReplayTracker = new Tracker({
          projectKey: openReplayApiKey,
          __DISABLE_SECURE_MODE:
            process.env.NODE_ENV === 'development' ? true : undefined,
        });
        setOpenReplayTracker(window.openReplayTracker);
        window.openReplayTracker.start();
      });
    }
    setOpenReplayTracker(window.openReplayTracker);
  }, [config?.clientSentryDsn, config?.openReplayApiKey]);

  useEffect(() => {
    if (userData && openReplayTracker) {
      openReplayTracker.setUserID(userData.username);
      setTimeout(() => {
        openReplayTracker?.setUserID(userData.username);
      }, 2000);
    }
  }, [userData, openReplayTracker]);

  useGlobalLoading(isFetching || isLoading);

  if (isLoading || (allowPrivate && !preferredOrganization)) {
    return null;
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

const GlobalLimitPopover = () => {
  const { planLimitErrors } = useOrganizationUsage();
  const [popoverOpen, setPopoverOpen] = useState(false);
  const handleClose = () => setPopoverOpen(false);

  useEffect(() => {
    if (planLimitErrors === 1) {
      setPopoverOpen(true);
    }
  }, [planLimitErrors]);

  const { preferredOrganization } = usePreferredOrganization();

  return preferredOrganization ? (
    <PlanLimitPopover open={popoverOpen} onClose={handleClose} />
  ) : null;
};

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

const Head: FC = () => {
  const theme = useTheme();

  return (
    <Helmet>
      <meta name="theme-color" content={theme.palette.navbarBackground.main} />
    </Helmet>
  );
};

export class App extends React.Component {
  componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
    errorActions.globalError.dispatch(error as GlobalError);
    throw error;
  }

  render() {
    return (
      <>
        <Head />
        <BrowserRouter>
          <Redirection />
          <Chatwoot />
          <MandatoryDataProvider>
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
              <PrivateRoute exact path={LINKS.ROOT.template}>
                <Redirect to={LINKS.PROJECTS.template} />
              </PrivateRoute>
              <PrivateRoute path={LINKS.PROJECTS.template}>
                <ProjectsRouter />
              </PrivateRoute>
              <PrivateRoute path={LINKS.USER_SETTINGS.template}>
                <UserSettingsRouter />
              </PrivateRoute>
              <PrivateRoute path={`${LINKS.ORGANIZATIONS.template}`}>
                <OrganizationsRouter />
              </PrivateRoute>
            </Switch>
            <SnackBar />
            <GlobalConfirmation />
            <GlobalLimitPopover />
          </MandatoryDataProvider>
        </BrowserRouter>
      </>
    );
  }
}
