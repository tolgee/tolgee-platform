import * as React from 'react';
import {useEffect, useState} from 'react';
import {GlobalActions} from '../store/global/globalActions';
import SnackBar from './common/SnackBar';
import {BrowserRouter, Redirect, Route, Switch} from 'react-router-dom';
import {container} from 'tsyringe';
import {useSelector} from 'react-redux';
import {AppState} from '../store';
import {LINKS} from '../constants/links';
import {PrivateRoute} from './common/PrivateRoute';
import {ErrorActions} from '../store/global/errorActions';
import {RedirectionActions} from '../store/global/redirectionActions';
import {useConfig} from "../hooks/useConfig";
import {useUser} from "../hooks/useUser";
import {ApiKeysView} from "./security/apiKeys/ApiKeysView";
import {UserSettings} from "./views/UserSettings";
import {RepositoriesRouter} from "./views/repositories/RepositoriesRouter";
import {FullPageLoading} from "./common/FullPageLoading";
import * as Sentry from '@sentry/browser';
import {GlobalError} from "../error/GlobalError";

const LoginRouter = React.lazy(() => import(/* webpackChunkName: "login" */'./security/LoginRouter'));
const SignUpView = React.lazy(() => import(/* webpackChunkName: "sign-up-view" */'./security/SignUpView'));

const PasswordResetSetView = React.lazy(() => import(/* webpackChunkName: "reset-password-set-view" */'./security/ResetPasswordSetView'));
const PasswordResetView = React.lazy(() => import(/* webpackChunkName: "reset-password-view" */'./security/ResetPasswordView'));
const AcceptInvitationHandler = React.lazy(() => import(/* webpackChunkName: "accept-invitation-handler" */'./security/AcceptInvitationHandler'));
const ConfirmationDialog = React.lazy(() => import(/* webpackChunkName: "confirmation-dialog" */'./common/ConfirmationDialog'));

const errorActions = container.resolve(ErrorActions);
const redirectionActions = container.resolve(RedirectionActions);

const Redirection = () => {
    let redirectionState = useSelector((state: AppState) => state.redirection);

    useEffect(() => {
        if (redirectionState.to) {
            redirectionActions.redirectDone.dispatch();
        }
    });

    if (redirectionState.to) {
        return <Redirect to={redirectionState.to}/>;
    }

    return null;
};

const MandatoryDataProvider = (props) => {
    let config = useConfig();
    let userData = useUser();

    useEffect(() => {
        if (config?.clientSentryDsn) {
            Sentry.init({dsn: config.clientSentryDsn});
            console.info("Using Sentry!");
        }
    }, [config?.clientSentryDsn])

    let allowPrivate = useSelector((state: AppState) => state.global.security.allowPrivate);

    if (!config || (!userData && allowPrivate) && config.authentication) {
        return <FullPageLoading/>
    } else {
        return props.children;
    }
};

const GlobalConfirmation = () => {

    let state = useSelector((state: AppState) => state.global.confirmationDialog);

    const [wasDisplayed, setWasDisplayed] = useState(false);

    let actions = container.resolve(GlobalActions);

    const onCancel = () => {
        state.onCancel?.();
        actions.closeConfirmation.dispatch();
    };

    const onConfirm = () => {
        state.onConfirm?.();
        actions.closeConfirmation.dispatch();
    };

    useEffect(() => {
        setWasDisplayed(wasDisplayed || !!state)
    }, [!state]);

    if (!wasDisplayed) {
        return null;
    }

    return (<ConfirmationDialog open={!!state} {...state} onCancel={onCancel} onConfirm={onConfirm}/>);
};

export class App extends React.Component {
    componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
        errorActions.globalError.dispatch(error as GlobalError);
        throw error;
    }

    render() {
        return (
            <BrowserRouter>
                <Redirection/>
                <MandatoryDataProvider>
                    <Switch>
                        <Route exact path={LINKS.RESET_PASSWORD_REQUEST.template}>
                            <PasswordResetView/>
                        </Route>
                        <Route exact path={LINKS.RESET_PASSWORD_WITH_PARAMS.template}>
                            <PasswordResetSetView/>
                        </Route>
                        <Route exact path={LINKS.SIGN_UP.template}>
                            <SignUpView/>
                        </Route>
                        <Route path={LINKS.LOGIN.template}>
                            <LoginRouter/>
                        </Route>
                        <Route path={LINKS.ACCEPT_INVITATION.template}>
                            <AcceptInvitationHandler/>
                        </Route>
                        <PrivateRoute exact path={LINKS.ROOT.template}>
                            <Redirect to={LINKS.REPOSITORIES.template}/>
                        </PrivateRoute>
                        <PrivateRoute exact path={LINKS.USER_SETTINGS.template}>
                            <UserSettings/>
                        </PrivateRoute>
                        <PrivateRoute path={LINKS.REPOSITORIES.template}>
                            <RepositoriesRouter/>
                        </PrivateRoute>
                        <PrivateRoute path={`${LINKS.USER_API_KEYS.template}`}>
                            <ApiKeysView/>
                        </PrivateRoute>
                    </Switch>
                    <SnackBar/>
                    <GlobalConfirmation/>
                </MandatoryDataProvider>
            </BrowserRouter>
        );
    }
}