import { FunctionComponent, useEffect } from 'react';
import { Redirect, useHistory, useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';

import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';

interface OAuthRedirectionHandlerProps {}
const LOCAL_STORAGE_STATE_KEY = 'oauth2State';

export const OAuthRedirectionHandler: FunctionComponent<
  OAuthRedirectionHandlerProps
> = () => {
  const allowPrivate = useGlobalContext((c) => c.auth.allowPrivate);
  const loginLoadable = useGlobalContext((c) => c.auth.authorizeOAuthLoadable);

  const { loginWithOAuthCode } = useGlobalActions();
  const match = useRouteMatch();
  const history = useHistory();

  useEffect(() => {
    const url = new URLSearchParams(window.location.search);
    const code = url.get('code');

    if (match.params[PARAMS.SERVICE_TYPE] == 'oauth2') {
      const state = url.get('state');
      const storedState = localStorage.getItem(LOCAL_STORAGE_STATE_KEY);
      if (storedState !== state) {
        history.replace(LINKS.LOGIN.build());
        return;
      } else {
        localStorage.removeItem(LOCAL_STORAGE_STATE_KEY);
      }
    }

    if (code && !allowPrivate) {
      loginWithOAuthCode(match.params[PARAMS.SERVICE_TYPE], code);
    }
  }, [allowPrivate]);

  if (loginLoadable.error) {
    return (
      <Redirect to={LINKS.LOGIN.build()}>
        <FullPageLoading />
      </Redirect>
    );
  }

  return <FullPageLoading />;
};
