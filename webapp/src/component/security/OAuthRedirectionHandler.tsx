import { FunctionComponent, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Redirect, useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { globalActions, GlobalState } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';
import { FullPageLoading } from '../common/FullPageLoading';

interface OAuthRedirectionHandlerProps {}

export const OAuthRedirectionHandler: FunctionComponent<OAuthRedirectionHandlerProps> =
  (props) => {
    const security = useSelector<AppState, GlobalState['security']>(
      (state) => state.global.security
    );

    const match = useRouteMatch();

    useEffect(() => {
      const code = new URLSearchParams(window.location.search).get('code');
      if (code && !security.allowPrivate) {
        globalActions.oAuthSuccessful.dispatch(
          match.params[PARAMS.SERVICE_TYPE],
          code
        );
      }
    }, [security.allowPrivate]);

    if (security.jwtToken) {
      return <Redirect to={LINKS.AFTER_LOGIN.build()} />;
    }

    if (security.loginErrorCode) {
      return <Redirect to={LINKS.LOGIN.build()} />;
    }

    return (
      <>
        <FullPageLoading />
      </>
    );
  };
