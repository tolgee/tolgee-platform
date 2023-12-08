import { FunctionComponent, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { Redirect, useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { globalActions, GlobalState } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';

import { FullPageLoading } from 'tg.component/common/FullPageLoading';

interface OAuthRedirectionHandlerProps {}
const LOCAL_STORAGE_STATE_KEY = 'oauth2State';

export const OAuthRedirectionHandler: FunctionComponent<
  OAuthRedirectionHandlerProps
> = (props) => {
  const security = useSelector<AppState, GlobalState['security']>(
    (state) => state.global.security
  );

  const [invalidState, setInvalidState] = useState(false);

  const match = useRouteMatch();

  useEffect(() => {
    const url = new URLSearchParams(window.location.search);
    const code = url.get('code');

    if (match.params[PARAMS.SERVICE_TYPE] == 'oauth2') {
      const state = url.get('state');
      const storedState = localStorage.getItem(LOCAL_STORAGE_STATE_KEY);
      if (storedState !== state) {
        setInvalidState(true);
        return;
      } else {
        localStorage.removeItem(LOCAL_STORAGE_STATE_KEY);
      }
    }

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

  if (security.loginErrorCode || invalidState) {
    return <Redirect to={LINKS.LOGIN.build()} />;
  }

  return (
    <>
      <FullPageLoading />
    </>
  );
};
