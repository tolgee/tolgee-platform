import {default as React, FunctionComponent, useEffect} from 'react';
import {Redirect, useRouteMatch} from 'react-router-dom';
import {container} from 'tsyringe';
import {GlobalActions, GlobalState} from '../../store/global/globalActions';
import {LINKS, PARAMS} from '../../constants/links';
import {useSelector} from 'react-redux';
import {AppState} from '../../store';
import {FullPageLoading} from "../common/FullPageLoading";

interface OAuthRedirectionHandlerProps {
}

const actions = container.resolve(GlobalActions);

export const OAuthRedirectionHandler: FunctionComponent<OAuthRedirectionHandlerProps> = (props) => {
    const security = useSelector<AppState, GlobalState['security']>((state) => state.global.security);

    const match = useRouteMatch();

    const code = new URLSearchParams(window.location.search).get('code');

    useEffect(() => {
        if (code && !security.allowPrivate) {
            actions.oAuthSuccessful.dispatch(match.params[PARAMS.SERVICE_TYPE], code);
        }
    }, [code, !!security.allowPrivate]);

    if (security.jwtToken) {
        return (<Redirect to={LINKS.AFTER_LOGIN.build()}/>);
    }

    if (security.loginErrorCode) {
        return (<Redirect to={LINKS.LOGIN.build()}/>);
    }


    return (
        <>
            <FullPageLoading/>
        </>
    );
};
