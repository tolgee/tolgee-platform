import { FunctionComponent, useEffect } from 'react';
import { useRouteMatch } from 'react-router-dom';
import { GlobalState } from '../../store/global/GlobalActions';
import { LINKS, PARAMS } from '../../constants/links';
import { useSelector } from 'react-redux';
import { AppState } from '../../store';
import { FullPageLoading } from '../common/FullPageLoading';
import { container } from 'tsyringe';
import { SignUpActions } from '../../store/global/SignUpActions';
import { RedirectionActions } from '../../store/global/RedirectionActions';
import { Loadable } from '../../store/AbstractLoadableActions';

interface OAuthRedirectionHandlerProps {}

const actions = container.resolve(SignUpActions);
const redirectionActions = container.resolve(RedirectionActions);

export const EmailVerificationHandler: FunctionComponent<OAuthRedirectionHandlerProps> =
  (props) => {
    const security = useSelector<AppState, GlobalState['security']>(
      (state) => state.global.security
    );
    const match = useRouteMatch();

    const verifyEmailLoadable = useSelector<AppState, Loadable>(
      (state) => state.signUp.loadables.verifyEmail
    );

    useEffect(() => {
      if (!verifyEmailLoadable.touched) {
        actions.loadableActions.verifyEmail.dispatch(
          match.params[PARAMS.USER_ID],
          match.params[PARAMS.VERIFICATION_CODE]
        );
      }
    }, []);

    useEffect(() => {
      if (security.allowPrivate && verifyEmailLoadable.loaded) {
        // userActions.loadableReset.userData.dispatch();
        redirectionActions.redirect.dispatch(LINKS.AFTER_LOGIN.build());
      }
    }, [security.allowPrivate, verifyEmailLoadable.loaded]);

    return (
      <>
        <FullPageLoading />
      </>
    );
  };
