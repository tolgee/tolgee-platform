import {default as React, FunctionComponent, useEffect} from 'react';
import {useRouteMatch} from 'react-router-dom';
import {GlobalState} from '../../store/global/globalActions';
import {LINKS, PARAMS} from '../../constants/links';
import {useSelector} from 'react-redux';
import {AppState} from '../../store';
import {FullPageLoading} from "../common/FullPageLoading";
import {container} from 'tsyringe';
import {SignUpActions} from "../../store/global/signUpActions";
import {RedirectionActions} from "../../store/global/redirectionActions";

interface OAuthRedirectionHandlerProps {
}

const actions = container.resolve(SignUpActions)
const redirectionActions = container.resolve(RedirectionActions)

export const EmailVerificationHandler: FunctionComponent<OAuthRedirectionHandlerProps> = (props) => {

    const security = useSelector<AppState, GlobalState['security']>((state) => state.global.security);
    const match = useRouteMatch();

    useEffect(() => {
        if (!security.allowPrivate) {
            actions.loadableActions.verifyEmail.dispatch(match.params[PARAMS.USER_ID], match.params[PARAMS.VERIFICATION_CODE]);
        }
    }, []);

    useEffect(() => {
        if (security.allowPrivate) {
            redirectionActions.redirect.dispatch(LINKS.AFTER_LOGIN.build())
        }
    }, [security.allowPrivate])

    return (
        <>
            <FullPageLoading/>
        </>
    );
};
